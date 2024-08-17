package com.tulrfsd.polarion.workflows.testruns.functions;

import java.util.HashSet;
import java.util.Set;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRecord;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.subterra.base.data.model.ICustomField;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IPrimitiveType;
import com.polarion.subterra.base.data.model.IType;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class AddTestCasesFromCollection implements IFunction<ITestRun> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + AddTestCasesFromCollection.class.getSimpleName();
  
  private ITestRun testRun;
  private ITestManagementService testMgmtService;
  private Set<String> compareEnumFieldIds;
  private Set<String> testRunStatusIdFilter;
  private Set<String> testRecordResultIdFilter;
  private int testRecordLimit;
  
  public void execute(ICallContext<ITestRun> context, IArguments arguments) {
    
    this.testRun = context.getTarget();
    this.testMgmtService = this.testRun.getTestManagementService();

    final String collectionFieldId = arguments.getAsString("collection.field.id");
    final String documentFieldId = arguments.getAsString("document.field.id");
    final String deltaModeFieldId = arguments.getAsString("delta.mode.field.id");
    this.compareEnumFieldIds = arguments.getAsSetOptional("compare.enum.field.ids");
    this.testRunStatusIdFilter = arguments.getAsSetOptional("testrun.status.filter");
    this.testRecordResultIdFilter = arguments.getAsSetOptional("testrecord.status.filter");
    this.testRecordLimit = arguments.getAsInt("test.records.limit", 999999);
    
    if (testRecordLimit < 1) {
      throw new UserFriendlyRuntimeException("The optional test record limit must not be smaller than 1. The default value is 999,999.");
    }

    boolean deltaMode = isDeltaMode(deltaModeFieldId);
    
    if (deltaMode) {
      for (String enumFieldId : compareEnumFieldIds) {
        if (!isEnumField(this.testRun, enumFieldId)) {
          throw new  UserFriendlyRuntimeException(String.format("The field with ID %s is not an enumeration. Please check the configuration of the workflow function %s.", enumFieldId, FUNCTION_NAME));
        }
      }
    }
    
    IBaselineCollection collection = WorkflowObjectCoreHelper.getCollectionFromEnumField( testRun, collectionFieldId);
    if (collection == null) {
      throw new UserFriendlyRuntimeException("No collection specified or the collection is unresolvable.");
    }
    IModule module = WorkflowObjectCoreHelper.getModuleFromEnumFieldAndCollection(testRun, documentFieldId, collection);
        
    performFunction(deltaMode, module);
  }
  
  
  private boolean isDeltaMode(String deltaModeFieldId) {
    
    IType fieldType = testRun.getCustomFieldPrototype(deltaModeFieldId).getType();
    if (!(fieldType instanceof IPrimitiveType primitiveType) || !primitiveType.getTypeName().equals("java.lang.Boolean")) {
      throw new UserFriendlyRuntimeException("The field to specify the test run mode (full or delta) is not defined or is not a boolean.");
    }
    return (boolean) testRun.getValue(deltaModeFieldId);
  }
  
  
  private boolean isEnumField(IPObject pObject, String enumFieldId) {
    ICustomField fieldPrototype = pObject.getCustomFieldPrototype(enumFieldId);
    return fieldPrototype.getType() instanceof IEnumType ||
        (fieldPrototype.getType() instanceof IListType listType && listType.getItemType() instanceof IEnumType);
  }
  
  
  private void performFunction(boolean deltaMode, IModule module) {
    testRun.getAllRecords().clear();
    
    // add currently defined entries
    for (IWorkItem workItem : module.getContainedWorkItems()) {
      if (testRun.getTrackerService().findWorkItem(workItem.getProjectId(), workItem.getId()).isUnresolvable()) {
        throw new UserFriendlyRuntimeException("The work item " + workItem.getId() + " does not exist anymore in HEAD revision and can thus not be added to the test run.");
      }
      
      if (workItem.getTestCase().getTestSteps() == null) {
        // work item does not contain test steps
        continue;
      }
      
      if (!deltaMode || testCaseRequiredForDeltaMode(workItem)) {
        testRun.addRecord().setTestCaseWithRevision(workItem);
      }
    }
    
    if (testRun.getAllRecords().isEmpty()) {
      throw new UserFriendlyRuntimeException("No test records could be added to the test run because none matched the configuration or none need to executed again due to newer revisions.");
    }
  }


  private boolean testCaseRequiredForDeltaMode(IWorkItem workItem) {
    
    if (workItem.getRevision() == null) {
      workItem = workItem.getTrackerService().getWorkItemWithRevision(workItem.getProjectId(), workItem.getId(), workItem.getLastRevision());
    }
    
    for (ITestRecord testRecord : this.testMgmtService.getLastTestRecords(workItem, testRecordLimit)) {
      
      // check if test record result and test run status match configuration
      if (!(this.testRecordResultIdFilter.isEmpty() || this.testRecordResultIdFilter.contains(testRecord.getResult().getId())) ||
          !(this.testRunStatusIdFilter.isEmpty() || this.testRunStatusIdFilter.contains(testRecord.getTestRun().getStatus().getId())) ||
          testRecord.getTestRun().equals(this.testRun)) {
        continue;
      }
      
      if (workItem.getRevision().equals(testRecord.getTestCaseWithRevision().getRevision()) &&
          enumFieldValuesFound(testRecord.getTestRun())) {
        // existing test record was executed for correct test case revision and
        // the specified enum entries of the test run were also selected in the old test run
        return false;
      }
    }
    return true;
  }

  
  private boolean enumFieldValuesFound(ITestRun oldTestRun) {
    
    for (String enumFieldId : this.compareEnumFieldIds) {
      Object currentFieldValue = this.testRun.getValue(enumFieldId);
      if (currentFieldValue == null) {
        // field is empty in current test run and thus disregarded
        continue;
      }
      
      if (!isEnumField(oldTestRun, enumFieldId)) {
        return false;
      }
      Object oldFieldValue = oldTestRun.getValue(enumFieldId);
      if (oldFieldValue == null) {
        return false;
      }

       Set<EnumOption> currentSet = convertEnumToSet(currentFieldValue);
       Set<EnumOption> oldSet = convertEnumToSet(oldFieldValue);
       if (!oldSet.containsAll(currentSet)) {
        return false;
      }
    }
    return true;
  }

  
  @SuppressWarnings("unchecked")
  private Set<EnumOption> convertEnumToSet(Object fieldValue) {
    Set<EnumOption> set = new HashSet<>();
    if (fieldValue instanceof EnumOption enumOption) {
      set.add(enumOption);
    } else if (fieldValue instanceof CustomTypedList customTypedList) {
      set.addAll(customTypedList);
    } else {
      throw new UserFriendlyRuntimeException(String.format("The method convertEnumToSet was handed a field value that is not an enumeration. Please check the configuration of the workflow function %s.", FUNCTION_NAME));
    }
    return set;
  }
}

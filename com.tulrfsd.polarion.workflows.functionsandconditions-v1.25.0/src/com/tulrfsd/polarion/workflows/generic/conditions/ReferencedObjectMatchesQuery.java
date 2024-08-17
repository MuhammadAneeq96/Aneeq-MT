package com.tulrfsd.polarion.workflows.generic.conditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.platform.persistence.spi.EnumOption;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ReferencedObjectMatchesQuery implements ICustomWorkflowCondition<IWorkflowObject>{
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ReferencedObjectMatchesQuery.class.getSimpleName();
  Set<String> fieldIds;
  String query;
  String message;

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {

    IWorkflowObject workflowObject = context.getTarget();

    fieldIds = new HashSet<>(arguments.getAsSet("field.ids"));
    query = arguments.getAsString("query");
    final boolean projectScope = arguments.getAsBoolean("scope.project", true);
    message = arguments.getAsString("message", "");

    if (query.isBlank()) {
      return String.format("The query parameter in the workflow condition %s must not be empty.", CONDITION_NAME);
    } else if (projectScope) {
      query = String.format("(%s) AND project.id:(%s)", query, workflowObject.getProjectId());
    }

    return processFields(workflowObject);
  }

  @SuppressWarnings("unchecked")
  private String processFields(IWorkflowObject workflowObject) {
    List<IEnumOption> list = new ArrayList<>();
    handleSpecialFields(workflowObject, fieldIds, list);
    
    for (String fieldId : fieldIds) {
      Object fieldValue = workflowObject.getValue(fieldId);
      
      if (!WorkflowObjectCoreHelper.isFieldDefined(workflowObject, fieldId)) {
        return String.format("The field with the ID %s is not defined. Please check the configuration of the %s workflow condition.", fieldId, CONDITION_NAME);
      } else if (fieldValue == null || (fieldValue instanceof Collection<?> collectionValue && collectionValue.isEmpty())) {
        continue;
      }
      
      if (fieldValue instanceof IEnumOption enumOptionValue) {
        list.add(enumOptionValue);
      } else if (fieldValue instanceof CustomTypedList listValue) {
        list.addAll(listValue);
      } else {
        return String.format("The field with the ID %s is not an enum field. Please check the configuration of the %s workflow condition.", fieldId, CONDITION_NAME);
      } 
    }
    
    return checkList(list);
  }

  private void handleSpecialFields(IWorkflowObject workflowObject, Set<String> fieldIds, List<IEnumOption> list) {
    
    // document field of test runs
    if (fieldIds.contains("document") && workflowObject instanceof ITestRun testRun && testRun.getDocument() != null) {
      IModule document = testRun.getDocument();
      String enumId = "@ProjectDocuments";
      String id = String.format("%s/%s", document.getProjectId(), document.getModuleLocation().getLocationPath());
      String name = document.getModuleNameWithSpace();
      Properties properties = new Properties();
      properties.clear();
      properties.setProperty(IEnumOption.PROPERTY_KEY_URI, document.getUri().toString());
      IEnumOption enumOption = new EnumOption(enumId, id, name, 0, false, properties);
      list.add(enumOption);
      fieldIds.remove("document");
    }
  }
  
  private String checkList(List<IEnumOption> list) {
    IDataService dataService = ServicesProvider.getDataService();
    for (IEnumOption enumOption : list) {
      IPObject object = dataService.getObjectFromEnumOption(enumOption);
      if (object == null) {
        return String.format("The enum value %s cannot be resolved to an object for the workflow condition %s.", enumOption.getId(), CONDITION_NAME);
      }
      String customQuery;
      if (object instanceof IWorkItem workItem) {
        customQuery = String.format("(%s) AND (project.id:(%s) AND id:(%s))", query, workItem.getProjectId(), workItem.getId());
      } else if (object instanceof IModule module) {
        customQuery = String.format("(%s) AND (project.id:(%s) AND space.id.1:(%s) AND id:(\"%s\"))", query, module.getProjectId(), module.getModuleFolder(), module.getId());
      } else if (object instanceof ITestRun testRun) {
        customQuery = String.format("(%s) AND (project.id:(%s) AND id:(\"%s\"))", query, testRun.getProjectId(), testRun.getId());
      } else {
        return String.format("The referenced object must be a WorkItem, Document (Module), or a TestRun. Please check the %s workflow condition configuration.", CONDITION_NAME);
      }
      IWorkflowObject workflowObject = (IWorkflowObject) object;
      int count = dataService.getInstancesCount(workflowObject.getPrototype(), WorkflowObjectCoreHelper.escapeHyphen(customQuery));
      
      if (count == 0) {
        return message.isBlank() ? String.format("The referenced %s %s is not part of the query defined in the %s workflow condition.", workflowObject.getPrototype().getName(), workflowObject.getId(), CONDITION_NAME) : message;
      } else {
        return null;
      }
    }
    return null;
  }

}

package com.tulrfsd.polarion.workflows.generic.conditions;

import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.IDataService;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class CheckForObjectsWithEqualFieldValues implements ICustomWorkflowCondition<IWorkflowObject>  {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + CheckForObjectsWithEqualFieldValues.class.getSimpleName();
  IWorkflowObject workflowObject;
  String query;
  boolean checkGlobal;
  boolean excludeWorkItems;
  boolean excludeDocuments;
  boolean excludeTestRuns;
  Set<String> fieldIdsEqual;
  Set<String> fieldIdsUnequal;

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    workflowObject = context.getTarget();
    query = arguments.getAsString("query");
    checkGlobal = arguments.getAsBoolean("check.global", false);
    excludeWorkItems = arguments.getAsBoolean("exclude.workitems", false);
    excludeDocuments = arguments.getAsBoolean("exclude.documents", false);
    excludeTestRuns = arguments.getAsBoolean("exclude.testruns", false);
    fieldIdsEqual = arguments.getAsSetOptional("field.ids.equal");
    fieldIdsUnequal = arguments.getAsSetOptional("field.ids.unequal");
    boolean useSystemUser = arguments.getAsBoolean("use.system.user", false);
    
    if (query.strip().isEmpty()) {
      throw new UserFriendlyRuntimeException(String.format("The query parameter for the workflow condition %s must not be empty.", CONDITION_NAME));
    }
    
    if (!checkGlobal) {
      query = String.format("(%s) AND project.id:(%s)", query, workflowObject.getProjectId());
    }
    
    try {
      if (useSystemUser) {
        return ServicesProvider.getSecurityService().doAsSystemUser((PrivilegedExceptionAction<String>) this::compareObjects);
      } else {
        return compareObjects();
      }
    } catch (UserFriendlyRuntimeException e) {
      return e.getMessage();
    } catch (Exception e) {
      e.printStackTrace();
      return String.format("Error occured while executing the workflow condition %s. Check the logs for details.", CONDITION_NAME);
    }
  }
  
  @SuppressWarnings("unchecked")
  private String compareObjects() {
    Set<IWorkflowObject> wObjects = new HashSet<>();
    IDataService dataService = ServicesProvider.getDataService();
    
    if (!excludeWorkItems) {
      wObjects.addAll(dataService.searchInstances(IWorkItem.PROTO, query, "id"));
    }
    if (!excludeDocuments) {
      wObjects.addAll(dataService.searchInstances(IModule.PROTO, query, "id"));
    }
    if (!excludeTestRuns) {
      wObjects.addAll(dataService.searchInstances(ITestRun.PROTO, query, "id"));
    }
    wObjects.remove(workflowObject);
    
    for (IWorkflowObject wObject : wObjects) {
      if (compareFields(workflowObject, wObject, fieldIdsEqual, true) &&
          compareFields(workflowObject, wObject, fieldIdsUnequal, false)) {
        return null;
      }
    }
    
    return String.format("No object could be found matching the configuration of the workflow condition %s", CONDITION_NAME);
  }
  
  private boolean compareFields(IWorkflowObject workflowObject, IWorkflowObject oWorkflowObject, Collection<String> fieldIds, boolean requireEqual) {
    for (String fieldId : fieldIds) {
      Object field1 = workflowObject.getValue(fieldId);
      Object field2 = oWorkflowObject.getValue(fieldId);
      
      if (field1 == null && field2 == null) {
        if (!requireEqual) {
          return false;
        }
      } else if (field1 == null) {
        if (requireEqual) {
          return false;
        }
      } else if (!field1.equals(field2) && requireEqual) {
        return false;
      }
    }
    return true;
  }
}

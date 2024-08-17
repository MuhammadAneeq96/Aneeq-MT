package com.tulrfsd.polarion.workflows.generic.conditions;

import java.security.PrivilegedExceptionAction;
import java.util.List;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class StatusAvailableForReferencingObject implements ICustomWorkflowCondition<IWorkflowObject>{
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + StatusAvailableForReferencingObject.class.getSimpleName();
  String prototypeId;
  String query;
  String targetStatusId;

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    
    IWorkflowObject workflowObject = context.getTarget();
    
    prototypeId = arguments.getAsString("prototype");
    query = arguments.getAsString("query");
    targetStatusId = arguments.getAsString("target.status.id");
    final boolean projectScope = arguments.getAsBoolean("scope.project", true);
    
    query = WorkflowObjectCoreHelper.resolveReferencingQuery(workflowObject, query, projectScope, CONDITION_NAME);
    query = WorkflowObjectCoreHelper.escapeHyphen(query);
    prototypeId = WorkflowObjectCoreHelper.getPrototypeId(prototypeId);
    final boolean useSystemUser = arguments.getAsBoolean("use.system.user", false);
    
    try {
      String failedObjectsText;
      if (useSystemUser) {
        failedObjectsText = ServicesProvider.getSecurityService().doAsSystemUser((PrivilegedExceptionAction<String>) this::checkReferencingObjects);
      } else {
        failedObjectsText = checkReferencingObjects();
      }
      if (!failedObjectsText.isEmpty()) {
        return String.format("The status with the ID \"%s\" is not available for the object(s):%n%s", targetStatusId, failedObjectsText);
      }
    } catch (UserFriendlyRuntimeException e) {
      return e.getMessage();
    } catch (Exception e) {
      e.printStackTrace();
      return String.format("Error occured while executing the workflow condition %s. Check the logs for details.", CONDITION_NAME);
    }
    
    return null;
  }
  
  private String checkReferencingObjects() {
    StringBuilder failedObjectsText = new StringBuilder();
    
    @SuppressWarnings("unchecked")
    List<IWorkflowObject> objects = ServicesProvider.getDataService().searchInstances(prototypeId, query, null);
    for (IWorkflowObject pObject : objects) {
      if (!WorkflowObjectCoreHelper.isActionAvailableForTargetStatus(pObject, targetStatusId)) {
        failedObjectsText.append(String.format("%s %s (%s).%n", pObject.getPrototype().getName(), pObject.getId(), pObject.getProject().getName()));
      }
    }
    return failedObjectsText.toString();
  }

}
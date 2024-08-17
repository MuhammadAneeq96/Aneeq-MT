package com.tulrfsd.polarion.workflows.generic.functions;

import java.security.PrivilegedExceptionAction;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import com.polarion.alm.tracker.model.IWorkflowAction;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.i18n.Localization;
import com.polarion.platform.security.PermissionDeniedException;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ExecuteReferencingObjectWorkflowAction implements IFunction<IWorkflowObject> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ExecuteReferencingObjectWorkflowAction.class.getSimpleName();
  ICallContext<IWorkflowObject> context;

  @Override
  public void execute(ICallContext<IWorkflowObject> context, IArguments arguments) {
    this.context = context;
    final String actionId = arguments.getAsString("action.id");
    final String targetStatusId = arguments.getAsString("target.status.id");
    GetReferencingObjectsAction action = new GetReferencingObjectsAction(context, arguments, FUNCTION_NAME);
    List<IWorkflowObject> objectList;
    try {
      objectList = ServicesProvider.getSecurityService().doAsSystemUser(action);
    } catch (UserFriendlyRuntimeException e) {
      throw new UserFriendlyRuntimeException(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      throw new UserFriendlyRuntimeException(e.getMessage());
    }
    
    processObjects(objectList, actionId, targetStatusId);
  }
  
  private void processObjects(@NotNull List<IWorkflowObject> list, @NotNull String actionId, @NotNull String targetStatusId) {
    for (IWorkflowObject object : list) {
      if (object == null) {
        throw new UserFriendlyRuntimeException(String.format("Internal error in %s workflow function. Object is null.", FUNCTION_NAME));
      } else if (object.isUnresolvable()) {
        throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to access the referencing object %s.", FUNCTION_NAME, object.getId()));
      } else if (!object.can().modifyKey("status")) {
        throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to modify the status of the referencing object %s.", FUNCTION_NAME, object.getId()));
      }
      
      IWorkflowAction action = WorkflowObjectCoreHelper.getAction(object.getAvailableActions(), actionId, targetStatusId);
      if (action == null) {
        throw new UserFriendlyRuntimeException(String.format("The action with the ID %s is not available for the %s %s.", actionId, object.getPrototype().getName(), object.getId()));
      } else if (!action.hasCurrentUserRequiredRoles(object.getContextId())) {
        throw new UserFriendlyRuntimeException(String.format("You do not have the required role to perform the action %s on the %s %s", action.getActionName(), object.getPrototype().getName(), object.getId()));
      }
      try {
        IWorkflowObject preparedObject = this.context.prepareObjectForModification(object);
        preparedObject.performAction(action.getActionId());
        preparedObject.save();
      } catch (PermissionDeniedException e) {
        throw new UserFriendlyRuntimeException(Localization.getString("form.workitem.message.cannotExecuteBecauseCannotChangeLinked"));
      }
    }
  }
}

class GetReferencingObjectsAction  implements PrivilegedExceptionAction<List<IWorkflowObject>> {
  
  ICallContext<IWorkflowObject> context;
  IArguments arguments;
  String functionName;
  
  GetReferencingObjectsAction(ICallContext<IWorkflowObject> context, IArguments arguments, String functionName) {
    this.context = context;
    this.arguments = arguments;
    this.functionName = functionName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<IWorkflowObject> run() throws Exception {
    String prototypeId = arguments.getAsString("prototype");
    String query = arguments.getAsString("query");
    final boolean projectScope = arguments.getAsBoolean("scope.project", false);
    
    IWorkflowObject workflowObject = context.getTarget();
    
    query = WorkflowObjectCoreHelper.resolveReferencingQuery(workflowObject, query, projectScope, functionName);
    prototypeId = WorkflowObjectCoreHelper.getPrototypeId(prototypeId);
    
    return ServicesProvider.getDataService().searchInstances(prototypeId, WorkflowObjectCoreHelper.escapeHyphen(query), null);
  }
  
}
package com.tulrfsd.polarion.workflows.generic.functions;

import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowAction;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.i18n.Localization;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.platform.security.PermissionDeniedException;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ExecuteReferencedObjectWorkflowAction implements IFunction<IWorkflowObject> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ExecuteReferencedObjectWorkflowAction.class.getSimpleName();
  
  ICallContext<IWorkflowObject> context;

  @Override
  public void execute(ICallContext<IWorkflowObject> context, IArguments arguments) {
    this.context = context;
    final String actionId = arguments.getAsString("action.id");
    final String targetStatusId = arguments.getAsString("target.status.id");
    GetReferencedObjectsAction action = new GetReferencedObjectsAction(context, arguments, FUNCTION_NAME);
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
        throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to access the referenced object %s.", FUNCTION_NAME, object.getId()));
      } else if (!object.can().modifyKey("status")) {
        throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to modify the status of the referenced object %s.", FUNCTION_NAME, object.getId()));
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

class GetReferencedObjectsAction  implements PrivilegedExceptionAction<List<IWorkflowObject>> {
  
  ICallContext<IWorkflowObject> context;
  IArguments arguments;
  String functionName;
  
  GetReferencedObjectsAction(ICallContext<IWorkflowObject> context, IArguments arguments, String functionName) {
    this.context = context;
    this.arguments = arguments;
    this.functionName = functionName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<IWorkflowObject> run() throws Exception {
    final Set<String> fieldIds = arguments.getAsSet("field.ids");
    String query = arguments.getAsString("query");
    final boolean projectScope = arguments.getAsBoolean("scope.project", false);
    
    IWorkflowObject workflowObject = context.getTarget();
    
    if (query.isBlank()) {
      throw new UserFriendlyRuntimeException(String.format("The query parameter in the workflow function %s must not be empty.", functionName));
    } else if (projectScope) {
      query = String.format("(%s) AND project.id:(%s)", query, workflowObject.getProjectId());
    }
    
    List<IEnumOption> list = new ArrayList<>();
    for (String fieldId : fieldIds) {
      Object fieldValue = workflowObject.getValue(fieldId);
      
      if (!WorkflowObjectCoreHelper.isFieldDefined(workflowObject, fieldId)) {
        throw new UserFriendlyRuntimeException(String.format("The field with the ID %s is not defined. Please check the configuration of the %s workflow function.", fieldId, functionName));
      } else if (fieldValue == null || (fieldValue instanceof Collection<?> collectionValue && collectionValue.isEmpty())) {
        continue;
      }
      
      if (fieldValue instanceof IEnumOption enumOption) {
        list.add(enumOption);
      } else if (fieldValue instanceof CustomTypedList listValue) {
        list.addAll(listValue);
      } else {
        throw new UserFriendlyRuntimeException(String.format("The field with the ID %s is not an enum field. Please check the configuration of the %s workflow function.", fieldId, functionName));
      } 
    }
    return getFilteredObjects(list, query);
  }
  
  @NotNull
  private List<IWorkflowObject> getFilteredObjects(@NotNull List<IEnumOption> enumList, @NotNull String query) {
    IDataService dataService = ServicesProvider.getDataService();
    
    List<IWorkflowObject> objectList = new ArrayList<>();
    for (IEnumOption enumOption : enumList) {
      IPObject object = dataService.getObjectFromEnumOption(enumOption);
      if (object == null) {
        throw new UserFriendlyRuntimeException(String.format("The enum value %s cannot be resolved to an object for the workflow function %s.", enumOption.getId(), functionName));
      }
      String customQuery;
      if (object instanceof IWorkItem workItem) {
        customQuery = String.format("(%s) AND (project.id:(%s) AND id:(%s))", query, workItem.getProjectId(), workItem.getId());
      } else if (object instanceof IModule module) {
        customQuery = String.format("(%s) AND (project.id:(%s) AND space.id.1:(%s) AND id:(\"%s\"))", query, module.getProjectId(), module.getModuleFolder(), module.getId());
      } else if (object instanceof ITestRun testRun) {
        customQuery = String.format("(%s) AND (project.id:(%s) AND id:(\"%s\"))", query, testRun.getProjectId(), testRun.getId());
      } else {
        throw new UserFriendlyRuntimeException(String.format("The referenced object must be a WorkItem, Document (Module), or a TestRun. Please check the %s workflow function configuration.", functionName));
      }
      IWorkflowObject workflowObject = (IWorkflowObject) object;
      int count = dataService.getInstancesCount(workflowObject.getPrototype(), WorkflowObjectCoreHelper.escapeHyphen(customQuery));
      if (count == 1) {
        objectList.add(workflowObject);
      } else if (count > 1) {
        throw new UserFriendlyRuntimeException(String.format("The %s %s is not referenced uniquely since it was found more than once by the workflow function %s.", workflowObject.getPrototype().getName(), workflowObject.getId(), functionName));
      }
    }
    return objectList;
  }
  
}

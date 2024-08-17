package com.tulrfsd.polarion.workflows.generic.conditions;

import java.security.PrivilegedExceptionAction;
import java.util.Map;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ReferencingObjectMatchesQuery implements ICustomWorkflowCondition<IWorkflowObject>{
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ReferencingObjectMatchesQuery.class.getSimpleName();
  String prototypeId;
  String query;

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    
    IWorkflowObject workflowObject = context.getTarget();
    
    prototypeId = arguments.getAsString("prototype");
    query = arguments.getAsString("query");
    final boolean projectScope = arguments.getAsBoolean("scope.project", true);
    final int min = arguments.getAsInt("min", 1);
    final int max = arguments.getAsInt("max", Integer.MAX_VALUE);
    final Map<String, String> regexMap = arguments.getArgumentsWithPrefix("regex.");
    final boolean requireAllPass = arguments.getAsBoolean("require.all.regex.pass", false);
    final String message = arguments.getAsString("message", "");
    final boolean useSystemUser = arguments.getAsBoolean("use.system.user", false);
    
    query = WorkflowObjectCoreHelper.resolveReferencingQuery(workflowObject, query, projectScope, CONDITION_NAME);
    query = WorkflowObjectCoreHelper.escapeHyphen(query);
    prototypeId = WorkflowObjectCoreHelper.getPrototypeId(prototypeId);
    
    if (!WorkflowObjectCoreHelper.fieldConditionsPass(workflowObject, regexMap, requireAllPass)) {
      return null;
    }
    
    try {
      int count;
      if (useSystemUser) {
        count = ServicesProvider.getSecurityService().doAsSystemUser((PrivilegedExceptionAction<Integer>) this::getInstancesCount);
      } else {
        count = getInstancesCount();
      }
      if (count < min) {
        return message.isBlank() ? String.format("Found %d references. The %s must be referenced by at least %d %s(s) matching the query \"%s\" in the workflow condition %s.", count, workflowObject.getPrototype().getName(), min, prototypeId, query, CONDITION_NAME) : message;
      } else if (count > max) {
        return message.isBlank() ? String.format("Found %d references. The %s must be referenced by at most %d %s(s) matching the query \"%s\" in the workflow condition %s.", count, workflowObject.getPrototype().getName(), max, prototypeId, query, CONDITION_NAME) : message;
      }
    } catch (UserFriendlyRuntimeException e) {
      return e.getMessage();
    } catch (Exception e) {
      e.printStackTrace();
      return String.format("Error occured while executing the workflow condition %s. Check the logs for details.", CONDITION_NAME);
    }
    
    return null;
  }
  
  private Integer getInstancesCount() {
    return ServicesProvider.getDataService().getInstancesCount(prototypeId, query);
  }

}
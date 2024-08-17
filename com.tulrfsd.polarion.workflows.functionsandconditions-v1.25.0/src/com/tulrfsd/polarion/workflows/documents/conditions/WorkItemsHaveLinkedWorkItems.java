package com.tulrfsd.polarion.workflows.documents.conditions;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.workflows.impl.ModuleLinkedWorkItemsCheckerAction;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class WorkItemsHaveLinkedWorkItems implements ICustomWorkflowCondition<IModule> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + WorkItemsHaveLinkedWorkItems.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IModule> context, IArguments arguments) {
    
    ModuleLinkedWorkItemsCheckerAction action = new ModuleLinkedWorkItemsCheckerAction(context.getTarget(),
        arguments.getAsSetOptional("workitem.source.type.ids"),
        arguments.getAsSetOptional("workitem.source.status.ids"),
        arguments.getAsSetOptional("workitem.target.type.ids"),
        arguments.getAsSetOptional("workitem.target.status.ids"),
        arguments.getAsSetOptional("linkrole.direct.ids"),
        arguments.getAsSetOptional("linkrole.back.ids"),
        arguments.getAsInt("min", 1),
        arguments.getAsInt("max", Integer.MAX_VALUE),
        arguments.getAsBoolean("include.referenced", true),
        arguments.getAsBoolean("ignore.suspect.links", false),
        arguments.getAsBoolean("scope.project", false),
        CONDITION_NAME);
    
    try {
      return ServicesProvider.getSecurityService().doAsSystemUser(action);
    } catch (UserFriendlyRuntimeException e) {
      return e.getMessage();
    } catch (Exception e) {
      e.printStackTrace();
      return String.format("Error occured while executing the workflow condition %s. Check the logs for details.", CONDITION_NAME);
    }
  }
}

package com.tulrfsd.polarion.workflows.documents.conditions;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.workflows.impl.ModuleTestCasesExecutedCheckerAction;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class TestCasesHaveTestRecords implements ICustomWorkflowCondition<IModule> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + TestCasesHaveTestRecords.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IModule> context, IArguments arguments) {

    ModuleTestCasesExecutedCheckerAction action =  new ModuleTestCasesExecutedCheckerAction(context.getTarget(), 
        arguments.getAsSetOptional("workitem.type.ids"),
        arguments.getAsSetOptional("workitem.status.ids"),
        arguments.getAsSetOptional("testrecord.result.ids"),
        arguments.getAsSetOptional("testrun.type.ids"),
        arguments.getAsSetOptional("testrun.status.ids"),
        arguments.getAsInt("min", 1),
        arguments.getAsInt("max", Integer.MAX_VALUE),
        arguments.getAsBoolean("include.referenced", true),
        arguments.getAsBoolean("scope.project", false),
        arguments.getAsBoolean("since.last.status.transition.only", false),
        arguments.getAsInt("search.limit", 999),
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

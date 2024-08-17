package com.tulrfsd.polarion.workflows.documents.functions;

import java.security.PrivilegedExceptionAction;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class EmptyRecycleBin implements IFunction<IModule> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + EmptyRecycleBin.class.getSimpleName();

  @Override
  public void execute(ICallContext<IModule> context, IArguments arguments) {
    boolean useSystemUser = arguments.getAsBoolean("use.system.user", false);
    
    EmptyRecycleBinAction action = new EmptyRecycleBinAction(context);
    try {
      if (useSystemUser) {
        context.getTrackerService().getDataService().getSecurityService().doAsSystemUser(action);
      } else {
        action.run();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new UserFriendlyRuntimeException(e);
    }
  }

}

class EmptyRecycleBinAction implements PrivilegedExceptionAction<Void> {
  ICallContext<IModule> context;
  IModule module;
  
  public EmptyRecycleBinAction(ICallContext<IModule> context) {
    this.context = context;
    this.module = context.getTarget();
  }

  @Override
  public Void run() throws UserFriendlyRuntimeException {
    module.getUnreferencedWorkItems().forEach(workItem -> {
      IWorkItem preparedWorkItem = context.prepareObjectForModification(workItem);
      module.removeWorkItem(preparedWorkItem);
    });
    return null;
  }
  
}
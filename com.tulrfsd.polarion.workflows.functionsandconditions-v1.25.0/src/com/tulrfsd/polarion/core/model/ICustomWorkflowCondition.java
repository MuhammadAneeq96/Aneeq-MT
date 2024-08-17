package com.tulrfsd.polarion.core.model;

import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IWorkflowCondition;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;

public interface ICustomWorkflowCondition<T extends IWorkflowObject> extends IWorkflowCondition<T> {
  
  @Override
  public default boolean passesCondition(ICallContext<T> context, IArguments arguments) {
    return false;
  }
  
  @Override
  public default String passesConditionWithFailureMessage(ICallContext<T> context, IArguments arguments) {
    try {
      return checkCondition(context, arguments);
    } catch (UserFriendlyRuntimeException e) {
      return e.getMessage();
    } catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }
  
  public String checkCondition(ICallContext<T> context, IArguments arguments);

}

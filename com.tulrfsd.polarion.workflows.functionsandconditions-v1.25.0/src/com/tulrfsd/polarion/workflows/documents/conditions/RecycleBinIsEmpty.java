package com.tulrfsd.polarion.workflows.documents.conditions;

import java.util.List;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class RecycleBinIsEmpty implements ICustomWorkflowCondition<IModule> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + RecycleBinIsEmpty.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IModule> context, IArguments arguments) {
    IModule module = context.getTarget();
    List<IWorkItem> recycleBin = module.getUnreferencedWorkItems();
    return recycleBin.isEmpty() ? null : String.format("The document's recycle bin contains %d work item(s). Please empty the recycle bin to continue.", recycleBin.size());
  }

}

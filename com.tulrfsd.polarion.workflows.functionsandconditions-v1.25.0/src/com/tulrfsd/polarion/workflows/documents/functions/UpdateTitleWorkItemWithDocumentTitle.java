package com.tulrfsd.polarion.workflows.documents.functions;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;

public class UpdateTitleWorkItemWithDocumentTitle implements IFunction<IModule> {

  @Override
  public void execute(ICallContext<IModule> context, IArguments arguments) {
    IModule module = context.getTarget();
    module.updateTitleHeading(module.getTitleOrName());
  }

}

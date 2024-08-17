package com.tulrfsd.polarion.workflows.generic.functions;

import java.util.Set;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.IBaselinesManager;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.platform.persistence.IDataService;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class CreateProjectBaseline implements IFunction<IWorkflowObject> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + CreateProjectBaseline.class.getSimpleName();
  
  @Override
  public void execute(ICallContext<IWorkflowObject> context, IArguments arguments) {

    IWorkflowObject workflowObject = context.getTarget();
    IUser user = workflowObject.getTrackerService().getProjectsService().getCurrentUser();
    IBaselinesManager baselinesManager = workflowObject.getTrackerService().getTrackerProject(workflowObject.getProject()).getBaselinesManager();
    IDataService dataService = workflowObject.getDataSvc();
    
    final String prefix = arguments.getAsString("prefix", "");
    final String suffix = arguments.getAsString("suffix", "");
    final Set<String> fieldIds = arguments.getAsSetOptional("field.ids");
    
    String baselineName = WorkflowObjectCoreHelper.generateName(workflowObject, prefix, suffix, fieldIds, FUNCTION_NAME);
    String userName = user.getName() == null ? user.getId() : user.getName();
    String baselineDescription = String.format("Automatically created by user %s during a status transition of %s %s.", userName, workflowObject.getPrototype().getName(), workflowObject.getId());
    
    baselinesManager.createBaseline(baselineName, baselineDescription, Integer.toString(Integer.valueOf(dataService.getLastStorageRevision().getName()) + 1), user).save();
  }

}

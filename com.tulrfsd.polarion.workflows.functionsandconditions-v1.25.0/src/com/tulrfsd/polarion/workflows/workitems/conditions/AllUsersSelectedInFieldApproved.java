package com.tulrfsd.polarion.workflows.workitems.conditions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class AllUsersSelectedInFieldApproved implements ICustomWorkflowCondition<IWorkItem>{
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + AllUsersSelectedInFieldApproved.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IWorkItem> context, IArguments arguments) {
    
    Set<String> fieldIds = arguments.getAsSet("field.ids");
    boolean ignoreAdditionalApprovals = arguments.getAsBoolean("ignore.additional.approvals", false);
    
    IWorkItem workItem = context.getTarget();
    List<IUser> users = WorkflowObjectCoreHelper.getUsersFromFields(workItem, fieldIds);
    
    @SuppressWarnings("unchecked")
    Set<IUser> approvalUsers = (Set<IUser>) workItem.getApprovals().stream()
        .filter(object -> ((IApprovalStruct) object).getStatus().getId().equals("approved"))
        .map(object -> ((IApprovalStruct) object).getUser())
        .collect(Collectors.toSet());
    
    if (!approvalUsers.containsAll(users)) {
      users.removeAll(approvalUsers);
      return String.format("The following users still need to approve the work item %s: %s",
          workItem.getId(), users.stream().map(IUser::getName).collect(Collectors.joining(", ")));
      
    } else if (!ignoreAdditionalApprovals && !users.containsAll(approvalUsers)) {
      approvalUsers.removeAll(users);
      return String.format("The following users approved the work item %s but were not supposed to: %s. Please remove these approvals before continuing.",
          workItem.getId(), approvalUsers.stream().map(IUser::getName).collect(Collectors.joining(", ")));
    }
    
    return null;
  }

}
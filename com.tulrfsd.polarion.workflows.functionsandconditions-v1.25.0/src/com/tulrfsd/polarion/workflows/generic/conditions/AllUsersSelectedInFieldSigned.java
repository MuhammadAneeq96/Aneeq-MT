package com.tulrfsd.polarion.workflows.generic.conditions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.model.signatures.ISignature;
import com.polarion.alm.tracker.model.signatures.ISignatureStateOpt;
import com.polarion.alm.tracker.model.signatures.ISignatureVerdictOpt;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignature;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.logging.Logger;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class AllUsersSelectedInFieldSigned implements ICustomWorkflowCondition<IWorkflowObject> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + AllUsersSelectedInFieldSigned.class.getSimpleName();
  private static final Logger logger = Logger.getLogger(AllUsersSelectedInFieldSigned.class);

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    final Set<String> fieldIds = arguments.getAsSet("field.ids");
    final String targetStatus = arguments.getAsString("target.status.id", "");

    IWorkflowObject workflowObject = context.getTarget();
    
    List<IUser> users = WorkflowObjectCoreHelper.getUsersFromFields(workflowObject, fieldIds);
    for (IWorkflowSignature workflowSignature : workflowObject.getWorkflowSignaturesManager().getSortedWorkflowSignatures()) {
      if (!targetStatus.isEmpty() && !workflowSignature.getTargetStatus().getId().equals(targetStatus)) {
        continue;
      }
      for (ISignature signature : workflowSignature.getSignatures()) {
        if (signature.getVerdict().getId().equals(ISignatureVerdictOpt.OPT_SIGNED) &&
            workflowSignature.getSignatureState().getId().equals(ISignatureStateOpt.OPT_DONE)) {
          users.remove(signature.getSignedBy());
        }
      }
    }
    return checkForRemainingUsers(users);
  }
  
  private String checkForRemainingUsers(List<IUser> users) {
    if(users == null) {
      logger.error("checkForRemainingUsers: users is null");
      throw new UserFriendlyRuntimeException("The workflow function AllUsersSelectedInFieldSigned encountered a NPE in checkForRemainingUsers(Set<IUser> users).");
    } else if (users.isEmpty()) {
      return null;
    } else {
      return "The workflow signature is still missing for the following user(s): " + users.stream().map(IUser::getName).collect(Collectors.joining(", "));
    }
  }


}

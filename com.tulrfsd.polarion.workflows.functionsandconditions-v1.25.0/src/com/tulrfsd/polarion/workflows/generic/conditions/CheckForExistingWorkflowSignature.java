package com.tulrfsd.polarion.workflows.generic.conditions;

import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.model.signatures.ISignature;
import com.polarion.alm.tracker.model.signatures.ISignatureStateOpt;
import com.polarion.alm.tracker.model.signatures.ISignatureVerdictOpt;
import com.polarion.alm.tracker.model.signatures.IWorkflowSignature;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.UserCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class CheckForExistingWorkflowSignature implements ICustomWorkflowCondition<IWorkflowObject> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + CheckForExistingWorkflowSignature.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    
    final boolean signatureShallExist = arguments.getAsBoolean("existing.signature", false);
    
    IWorkflowObject workflowObject = context.getTarget();
    
    boolean signatureExists = doesWorkflowSignatureExist(context.getTargetStatusId(), workflowObject);
    
    if (signatureShallExist && !signatureExists) {
      return "You can only sign when you have an already existing signature for the target status.";
    }
    if (!signatureShallExist && signatureExists) {
      return "You have already signed for the target status.";
    }
    
    return null;
  }
  
  private boolean doesWorkflowSignatureExist(String targetStatusId, IWorkflowObject workflowObject) {
    for (IWorkflowSignature workflowSignature : workflowObject.getWorkflowSignaturesManager().getSortedWorkflowSignatures()) {
      if (!workflowSignature.getTargetStatus().getId().equals(targetStatusId) ||
          !workflowSignature.getSignatureState().getId().equals(ISignatureStateOpt.OPT_DONE)) {
        continue;
      }
      ISignature signature = workflowSignature.getSignature(UserCoreHelper.getCurrentUser());
      if (signature != null && signature.getVerdict().getId().equals(ISignatureVerdictOpt.OPT_SIGNED)) {
        return true;
      }
    }
    return false;
  }

}

/**
 * 
 */
package com.polarion.verifyCEVersionID;

/**
 * 
 */

import com.polarion.alm.tracker.ITrackerPolicy;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IWorkflowCondition;
import com.polarion.platform.security.PermissionDeniedException;
/*import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.workflows.utils.Literals;*/
import java.util.Set;


public class VerifyCEVersionID implements IWorkflowCondition<IWorkItem> {
	
	/*static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + VerifyCEVersionID.class.getSimpleName();*/

	Set<String> linkedWorkItemtypeID;
	Set<String> linkedWorkItemID;
	IWorkItem currentWorkItem;
	IModule retrievedCD;
	boolean allTestsPassed = true;
	boolean ceVersionMatch = true;
	
	public void execute(ICallContext<IWorkItem> context, IArguments arguments) {
	    IWorkItem currentWorkItem = context.getTarget();
	    String currentWorkItemID = currentWorkItem.getId();
	    Object currentWorkItemVersionID = currentWorkItem.getCustomField('ceVersion');
	    // Object currentWorkItemVersionID = currentWorkItem.getValue('ceversion');
	    
	    currentWorkItem = context.getTarget();
	    module = workItem.getModule();
	    
	checkLinkedTestCaseResults(currentWorkItem);
	check
	}

	@Override
	public boolean passesCondition(ICallContext<IWorkItem> arg0, IArguments arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String passesConditionWithFailureMessage(ICallContext<IWorkItem> arg0, IArguments arg1) {
		// TODO Auto-generated method stub
		return null;
	}

}

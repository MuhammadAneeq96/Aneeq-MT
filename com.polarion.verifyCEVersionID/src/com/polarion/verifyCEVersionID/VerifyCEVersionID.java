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
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
//import com.tulrfsd.polarion.workflows.utils.Literals;
import java.util.Set;
import java.util.List;


public class VerifyCEVersionID implements ICustomWorkflowCondition<IWorkItem> {
	
	//static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + VerifyCEVersionID.class.getSimpleName();

	IWorkItem currentWorkItem; // gets the current work item (CE)
	Set<String> currentWorkItemID; // variable for storing the polarion ID of the current work item (CE)
	Set<String> currentWorkItemVersionID; // variable for storing the value of the field 'Version' of the current work item (CE)
	Set<String> linkedWorkItemtypeID; // variable for storing the type ID of linked work item types to the current work item (CE)
	// Set<String> linkedTestCaseWorkItemtypeID; // variable for storing the type ID of linked work item types to the current work item (CE)
	List<ILinkedWorkItemStruct> linkedTestCases;
	Set<String> linkedWorkItemID; // variable for storing the ID of all linked work item types to the current work item (CE)
	IWorkItem retrievedWorkItem;
	IModule retrievedCD;
	boolean allTestsPassed;
	boolean ceVersionMatch;
	
	//public void execute(ICallContext<IWorkItem> context, IArguments arguments) {
	@Override
	public String checkCondition(ICallContext<IWorkItem> context) {
	    IWorkItem currentWorkItem = context.getTarget();
	    String currentWorkItemID = currentWorkItem.getId();
	    Object currentWorkItemVersionID = currentWorkItem.getCustomField('ceVersion');
	    allTestsPassed = true;
		ceVersionMatch = true;
		
	    // Object currentWorkItemVersionID = currentWorkItem.getValue('ceversion');
	    
	    currentWorkItem = context.getTarget();
	    
	    
	    return false;
	}
	
	 private boolean checkAllTestsPassed() {
	
		 // Use allTestsPassed here in the condition
		 	 
	 }
	 
	 private boolean checkCEVersionMatch() {
		 
 		 // module = workItem.getModule();
		 // Use ceVersionMatch here in the condition
	 }
	
	
	//checkLinkedTestCaseResults(currentWorkItem);
		
	/*@Override
	public String passesConditionWithFailureMessage(ICallContext<IWorkItem> arg0, IArguments arg1) {
		// TODO Auto-generated method stub
		return null;
	}*/

}

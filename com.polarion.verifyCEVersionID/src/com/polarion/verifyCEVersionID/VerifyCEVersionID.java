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
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IWorkflowCondition;
import com.polarion.platform.security.PermissionDeniedException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
//import com.tulrfsd.polarion.workflows.utils.Literals;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;


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
	    Object currentWorkItemVersionID = currentWorkItem.getCustomField("ceVersion");
	    allTestsPassed = true;
		ceVersionMatch = true;
		
        checkAllTestsPassed();
        // condition2();
			    
	    //return false;
	}
	
	 private boolean checkAllTestsPassed() {	 
    	 List<ILinkedWorkItemStruct> linkedWorkItemsStructs = currentWorkItem.getLinkedWorkItemsStructBack();
    	 List<IWorkItem> linkedTestCases = linkedWorkItemsStructs.stream()
    	            .filter(linkedWorkItemStruct -> "tested_by".equals(linkedWorkItemStruct.getLinkRole().getId()))
    	            .map(ILinkedWorkItemStruct::getLinkedItem)  // Map to the linked work item
    	            .collect(Collectors.toList());  // Collect to a list
    	 
    	        //return linkedTestCases;
    	        
    	        for (IWorkItem testCase : linkedTestCases) {
    	            Optional<TestRecord> mostRecentTestRecord = getMostRecentTestRecord(testCase);

    	            if (mostRecentTestRecord.isPresent()) {
    	                String testRecordStatus = mostRecentTestRecord.get().getCustomField(TestRecordFields.STATUS);

    	                // Check if the status is not 'passed'
    	                if (!"passed".equalsIgnoreCase(testRecordStatus)) {
    	                    allTestsPassed = false;
    	                    break;  // Exit the loop as soon as one failed test is found
    	                }
    	            } else {
    	                // If there is no test record, consider it as a failed condition
    	                allTestsPassed = false;
    	                break;
    	            }
    	        }
    	    }
		 
	 private Optional<TestRecord> getMostRecentTestRecord(IWorkItem testCase) {
	        // Assuming testCase.getLinkedWorkItemsStructDirect() gives you the linked test records
	        List<TestRecord> testRecords = testCase.getLinkedWorkItemsStructsDirect().stream()
	                .filter(linkedWorkItemStruct -> linkedWorkItemStruct.getLinkedItem() instanceof TestRecord)
	                .map(linkedWorkItemStruct -> (TestRecord) linkedWorkItemStruct.getLinkedItem())
	                .collect(Collectors.toList());

	        // Get the most recent test record based on the created date
	        return testRecords.stream()
	                .max((record1, record2) -> {
	                    String created1 = record1.getCustomField(TestRecordFields.CREATED);
	                    String created2 = record2.getCustomField(TestRecordFields.CREATED);
	                    return created1.compareTo(created2);
	                });
	    }
	
		 // Use allTestsPassed here in the condition
		 // checkAllTestsPassed = false;
		 	 
	 private boolean checkCEVersionMatch() {
		 //if...

 		 // module = workItem.getModule();
		 // Use ceVersionMatch here in the condition
		 checkAllTestsPassed = false;
	 }
	
	
	//checkLinkedTestCaseResults(currentWorkItem);
		
	/*@Override
	public String passesConditionWithFailureMessage(ICallContext<IWorkItem> arg0, IArguments arg1) {
		// TODO Auto-generated method stub
		return null;
	}*/
}

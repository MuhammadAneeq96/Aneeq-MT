//  First commit by salman

// Create a log file for debugging the code
var FileWriter = Java.type('java.io.FileWriter'); 
var outFile = new FileWriter("./data/logs/main/verifyCEVersionID_v1_logs.log");
var BufferedWriter = Java.type('java.io.BufferedWriter'); 
var out = new BufferedWriter(outFile);

out.write("Running verifyCEVersionID.js script"); out.newLine(); out.flush();

let logger = com.polarion.core.util.logging.Logger.getLogger(arguments.getAsString("script"));
let workItem = workflowContext.getTarget();

// Retrieve the Version field value of the Configuration Element
var configElementVersion = workItem.getCustomField(version);

// Retrieve all linked work items with the link role 'tested by'
var linkedWorkItems = workItem.getLinkedWorkItems();
var allTestsPassed = true;
var ceVersionMatch = true;

try {
    for (var i = 0; i < linkedWorkItems.size(); i++) {
        var linkedWorkItem = linkedWorkItems.get(i);
        
        // Check if the linked work item is a Test Case
        if (linkedWorkItem.getType().getId() === "testcase") {
            // Retrieve the Test Records for the Test Case
           // var testRecordsField = linkedWorkItem.getCustomField("test-records"); // got this Test Records field ID from the Test Case Form Configuration in Administration
            var linkedTestCase = linkedWorkItem.get(i);     
            var linkedTestCaseId = linkedTestCase.getId();
           // var latestTestRecord = linkedTestCase.TestRecordFields.iteration(end); // get list of the latest test record for this test case
            var linkedTestCase = getRecordsforTestCase(linkedTestCaseId);
            var latestTestRecord = testRecordsList(testRecordsList.length - 1); // Retrieve the last test record

            // Check if the latest test iteration has passed
            if (latestTestRecord(end) != 'passed') {
                allTestsPassed = false;
                logger.info("Test case " + linkedWorkItem.getId() + " did not pass.");
                break;
            }

            // Retrieve the Test Runs associated with the Test Case
            var testRuns = linkedWorkItem.getTitle(); // have to check if this is the correct way to retrieve the Test Run from a test case
            for (var j = 0; j < testRuns.size(); j++) {
                var testRun = testRuns.get(j);
                
                // Retrieve the value from the custom field 'Config Document Rev Number':
                var configDocRevNumber = TestRun.fields.get("configDocRevNumber").get().id();

                // Get the Configuration Document from the revision
                var configDocRef = com.polarion.alm.shared.api.model.ModelObjectReference.getFromRevision(workflowContext.getReadOnlyTransaction(), configDocRevNumber);
                var configDoc = configDocRef.getDocument();
                if (configDoc === null) {
                    ceVersionMatch = false;
                    logger.info("Configuration Document with revision number " + configDocRevNumber + " not found.");
                    break;
                }
                
                // Check if we only retrieve the workitems of type "Configuration Element"
                var configElementsList = configDoc.getWorkItems("configuration_element");
                var matchingConfigElement = null;
                for (var k = 0; k < configElementsList.size(); k++) {
                    var configElement = configElementsList.get(k);
                    if (configElement.getId() === workItem.getId()) {
                        matchingConfigElement = configElement;
                        break;
                    }
                }

                if (matchingConfigElement === null) {
                    ceVersionMatch = false;
                    logger.info("Configuration Element " + workItem.getId() + " not found in Configuration Document " + configDocRevNumber + ".");
                    break;
                }
                
                // Check if the custom field matches the Version field of the Configuration Element
                var retrievedConfigElementVersion = matchingConfigElement.getCustomField("version");
                if (retrievedConfigElementVersion !== configElementVersion) {
                    ceVersionMatch = false;
                    logger.info("The Configuration Element version " + configDocRevNumber + " in document revision " + configDocRevNumber + " does not match the current Configuration Element version " + configElementVersion + ".");
                    break;
                }
            }

             // If any condition fails, stop checking further
             if ((allTestsPassed || ceVersionMatch) === 0) {
                break;
            }
        }
    }
} catch (e) {
    logger.error("An error occurred: " + e.message);
    allTestsPassed = false;
    ceVersionMatch = false;
}

// Determine the final condition result
var conditionsMatch = false;
var conditionsMatch = allTestsPassed && ceVersionMatch;

// Log the final result
if (conditionsMatch === true) {
    logger.info("All conditions matched successfully for this transition.");
} else {
    logger.info("Conditions did not match for this transition.");
}

// Return the result
conditionsMatch;

// Closing the Log File
out.close();
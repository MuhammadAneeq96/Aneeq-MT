BEGIN

    // Retrieve the current work item 
    GET the current work item 
    STORE in currentWorkitem 
    GET the ID of currentWorkitem
    STORE in currentWorkitemID
    
    // Retrieve the Version ID from the field value of the Configuration Element
    GET the version of the currentWorkitem
    STORE in currentConfigElementVersion

    // Retrieve all linked work items with the link role 'tested by'
    GET all linked work items with link role 'tested by'
    STORE in linkedWorkItems
    SET allTestsPassed to true
    SET ceVersionMatch to true

    TRY
        FOR each linked work item from linkedWorkItems
            STORE in linkedWorkItem
            IF the linkedWorkItem is a test case THEN
                SET linkedTestCase to linkedWorkItem  

                // Retrieve the rest records list for each linkedTestCase
                GET the test record list for linkedTestCase
                STORE in testRecordsField

                // Retrieve the latest test record for each linkedTestCase
                GET the testRecordsField(end)
                STORE in latestTestRecord

                // CHECK if the test has passed
                IF latestTestRecord is not Passed THEN
                    SET allTestsPassed to false
                    LOG ("Test case" + <linkedTestCase> + "did not pass.")
                    EXIT loop

                // RETRIEVE the Test Runs associated with the Test Case
                GET the Test Run from linkedTestCase
                STORE in testRun

                // RETRIEVE the value from the custom field 'Config Document Rev Number':
                GET the Configuration Document Revision Number from testRun
                STORE in configDocRevNumber

                // RETRIEVE the Configuration Document from the Revision
                GET the document against configDocRevNumber
                STORE in configDoc
                IF the document is not found THEN
                    SET IDMatch to false
                    LOG ("Configuration Document with revision number " + <configDocRevNumber> + " not found.")
                    EXIT loop

                // Get the configuration elements in the document
                GET the configuration elements from the configDoc
                STORE in retrievedconfigElementsList
                FOR each configuration element in retrievedconfigElementsList
                    STORE in retrievedconfigElement
                    GET ID of retrievedconfigElement
                    STORE in retrievedconfigElementID
                    // Check if the configuration element with the ID exists in the configuraiton document revision 
                    FOR each retrievedconfigElementID
                        IF retrievedconfigElementID is not equal to currentWorkitemID THEN
                            SET ceVersionMatch to false
                            LOG ("The retrieved configuration element cannot be found in the configuration document with the revision: " + </configDocRevNumber> + ".")
                            EXIT loop
                    GET version ID of configElement
                    STORE in retrievedConfigElementVersion
                    // Check if the versions of the current and retirieved work items match
                    IF retrievedConfigElementVersion is not equal to currentConfigElementVersion THEN
                        SET ceVersionMatch to false
                        LOG ("The current version ID: " + <currentConfigElementVersion> + " and the retrieved version ID: " + <retrievedConfigElementVersion> " of the configuration element" + <configElement> " do not match")
                        EXIT loop


                // Stop if any condition fails
                IF allTestsPassed OR ceVersionMatch are false THEN
                    EXIT loop
    CATCH any errors
        Log the error

    SET conditionsMatch to false
    SET conditionsMatch to (allTestsPassed & ceVersionMatch)
    // Check if both conditions meet
    IF conditionsMatch is false THEN
        LOG ("Condition for the transition meet. Transition possible.")
    ELSE
        LOG ("Condition for the transition do not meet. Transition not possible")

    // Return the final result
    Return conditionsMatch

    // Close the log file
    CLOSE log file
END

package com.tulrfsd.polarion.workflows.testruns.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.polarion.alm.tracker.model.ITestRecord;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class AllTestRecordsExecuted implements ICustomWorkflowCondition<ITestRun> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + AllTestRecordsExecuted.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<ITestRun> context, IArguments arguments) {
    ITestRun testRun = context.getTarget();
    Set<String> validResultIds = arguments.getAsSetOptional("valid.result.ids");
    
    List<ITestRecord> allResults = new ArrayList<>(testRun.getAllRecords());
    
    if (validResultIds.isEmpty()) {
      allResults.removeAll(testRun.getRecords());
    }
    for (String resultId : validResultIds) {
      allResults.removeAll(testRun.getRecordsForResult(resultId));
    }
    
    return allResults.isEmpty() ? null : String.format("The test run still contains %d not executed test record(s), or the test records don't have one of the valid results specified in the workflow condition %s.", allResults.size(), CONDITION_NAME);
  }

}

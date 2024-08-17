package com.tulrfsd.polarion.workflows.impl;

import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRecord;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.platform.persistence.IEnumOption;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;

public class ModuleTestCasesExecutedCheckerAction implements PrivilegedExceptionAction<String> {
  
  IModule module;
  Set<String> workItemTypeIds;
  Set<String> workItemStatusIds;
  Set<String> testRecordResultIds;
  Set<String> testRunTypeIds;
  Set<String> testRunStatusIds;
  boolean includeReferencedWorkItems;
  String projectId;
  boolean lastStatusTransitionOnly;
  int searchLimit;
  int min;
  int max;

  String conditionName;

  public ModuleTestCasesExecutedCheckerAction(@NotNull IModule module,
                                        @NotNull Set<String> workItemTypeIds,
                                        @NotNull Set<String> workItemStatusIds,
                                        @NotNull Set<String> testRecordResultIds,
                                        @NotNull Set<String> testRunTypeIds,
                                        @NotNull Set<String> testRunStatusIds,
                                        int min,
                                        int max,
                                        boolean includeReferencedWorkItems,
                                        boolean projectScope,
                                        boolean lastStatusTransitionOnly,
                                        int searchLimit,
                                        @NotNull String conditionName) {
    
    super();
    
    this.module = module;
    this.workItemTypeIds = workItemTypeIds;
    this.workItemStatusIds = workItemStatusIds;
    this.testRecordResultIds = testRecordResultIds;
    this.testRunTypeIds = testRunTypeIds;
    this.testRunStatusIds = testRunStatusIds;
    this.includeReferencedWorkItems = includeReferencedWorkItems;
    this.projectId = projectScope ? module.getProjectId() : null;
    this.lastStatusTransitionOnly = lastStatusTransitionOnly;
    this.searchLimit = searchLimit;
    this.min = min;
    this.max = max;

    this.conditionName = conditionName;
  }

  @Override
  @Nullable
  public String run() throws Exception {
    
    List<IWorkItem> workItems = module.getContainedWorkItems();
    if (!includeReferencedWorkItems) {
      workItems.removeAll(module.getExternalWorkItems());
    }
    
    Optional<IWorkItem> wrongWorkItem = workItems.stream()
                                                 .filter(this::isRelevantWorkItem)
                                                 .filter(this::hasWrongNumberOfTestRecords)
                                                 .findFirst();
    
    return wrongWorkItem.isEmpty() ? null : String.format("The work item %s does not have the required number of test records matching the configuration of the workflow condition %s.", wrongWorkItem.get().getId(), conditionName);
  }
  
  private boolean isRelevantWorkItem(@NotNull IWorkItem workItem) {
    
    return !workItem.isUnresolvable() &&
           (workItemTypeIds.isEmpty() || workItemTypeIds.contains(workItem.getType().getId())) &&
           (workItemStatusIds.isEmpty() || workItemStatusIds.contains(workItem.getStatus().getId()));
  }
  
  private boolean hasWrongNumberOfTestRecords(@NotNull IWorkItem workItem) {
    
    String minRevision = WorkflowObjectCoreHelper.getRevisionOfLastStatusChange(workItem, null);
    if (minRevision == null) {
      throw new UserFriendlyRuntimeException(String.format("Internal Error: The workflow condition %s could not determine the revision of the last status change of WorkItem %s.", conditionName, workItem.getId()));
    }
    
    long count = ServicesProvider.getTestManagementService()
                                 .getLastTestRecords(workItem, searchLimit)
                                 .stream()
                                 .filter(testRecord -> isValidTestRecord(testRecord, minRevision))
                                 .limit(searchLimit < max ? min : max + 1)
                                 .count();
    
    return count < min || count > max;
  }
  
  private boolean isValidTestRecord(@NotNull ITestRecord testRecord, @NotNull String minRevision) {
    
    ITestRun testRun = testRecord.getTestRun();
    IEnumOption testResult = testRecord.getResult();
    String testRevision = testRecord.getTestCaseWithRevision().getRevision();
    
    return  testRecord.isExecuted() &&
           (!lastStatusTransitionOnly || (testRevision != null && Integer.parseInt(testRevision) >= Integer.parseInt(minRevision))) &&
           (testRecordResultIds.isEmpty() || (testResult != null && testRecordResultIds.contains(testResult.getId()))) &&
           (testRunTypeIds.isEmpty() || testRunTypeIds.contains(testRun.getType().getId())) &&
           (testRunStatusIds.isEmpty() || testRunStatusIds.contains(testRun.getStatus().getId())) &&
           (projectId == null || testRun.getProjectId().equals(projectId));
  }

}

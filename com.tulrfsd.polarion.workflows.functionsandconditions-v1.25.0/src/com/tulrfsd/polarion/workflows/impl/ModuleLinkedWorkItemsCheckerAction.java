package com.tulrfsd.polarion.workflows.impl;

import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;


public class ModuleLinkedWorkItemsCheckerAction implements PrivilegedExceptionAction<String> {
  
  IModule module;
  Set<String> workItemSourceTypeIds;
  Set<String> workItemSourceStatusIds;
  Set<String> workItemTargetTypeIds;
  Set<String> workItemTargetStatusIds;
  Set<String> linkRoleDirectIds;
  Set<String> linkRoleBackIds;
  int min;
  int max;
  boolean includeReferencedWorkItems;
  boolean ignoreSuspectLinks;
  String projectId;
  String conditionName;
  
  public ModuleLinkedWorkItemsCheckerAction(@NotNull IModule module,
                                            @NotNull Set<String> workItemSourceTypeIds,
                                            @NotNull Set<String> workItemSourceStatusIds,
                                            @NotNull Set<String> workItemTargetTypeIds,
                                            @NotNull Set<String> workItemTargetStatusIds,
                                            @NotNull Set<String> linkRoleDirectIds,
                                            @NotNull Set<String> linkRoleBackIds,
                                                     int min,
                                                     int max,
                                                     boolean includeReferencedWorkItems,
                                                     boolean ignoreSuspectLinks,
                                                     boolean projectScope,
                                            @NotNull String conditionName) {
    
    super();
    
    this.module = module;
    this.workItemSourceTypeIds = workItemSourceTypeIds;
    this.workItemSourceStatusIds = workItemSourceStatusIds;
    this.workItemTargetTypeIds = workItemTargetTypeIds;
    this.workItemTargetStatusIds = workItemTargetStatusIds;
    this.linkRoleDirectIds = linkRoleDirectIds;
    this.linkRoleBackIds = linkRoleBackIds;
    this.min = min;
    this.max = max;
    this.includeReferencedWorkItems = includeReferencedWorkItems;
    this.ignoreSuspectLinks = ignoreSuspectLinks;
    this.projectId = projectScope ? module.getProjectId() : null;
    this.conditionName = conditionName;
  }
  
  @Override
  @Nullable
  public String run() throws Exception {
    
    if (linkRoleDirectIds.isEmpty() && linkRoleBackIds.isEmpty()) {
      return String.format("Either the direct link role IDs or the back link role IDs need to be defined in the workflow condition %s.", conditionName);
    }
        
    List<IWorkItem> workItems = module.getContainedWorkItems();
    if (!includeReferencedWorkItems) {
      workItems.removeAll(module.getExternalWorkItems());
    }
    
    Optional<IWorkItem> wrongWorkItem = workItems.stream()
                                                 .filter(this::isRelevantWorkItem)
                                                 .filter(this::hasWrongNumberOfLinks)
                                                 .findFirst();
    
    return wrongWorkItem.isEmpty() ? null : String.format("The work item %s does not have the required number of links matching the configuration of the workflow condition %s.", wrongWorkItem.get().getId(), conditionName);
  }
  
  private boolean isRelevantWorkItem(@NotNull IWorkItem workItem) {
    
    return !workItem.isUnresolvable() &&
           (workItemSourceTypeIds.isEmpty() || workItemSourceTypeIds.contains(workItem.getType().getId())) &&
           (workItemSourceStatusIds.isEmpty() || workItemSourceStatusIds.contains(workItem.getStatus().getId()));
  }
  
  
  private boolean hasWrongNumberOfLinks(@NotNull IWorkItem workItem) {
    
    int count = 0;
    
    if (!linkRoleDirectIds.isEmpty()) {
      count += workItem.getLinkedWorkItemsStructsDirect()
                       .stream()
                       .filter(linkedWIStruct -> isRelevantLinkedWorkItem(linkedWIStruct, linkRoleDirectIds))
                       .limit((long) max + 1)
                       .count();
    }
    
    if (!linkRoleBackIds.isEmpty()) {
      count += workItem.getLinkedWorkItemsStructsBack()
                       .stream()
                       .filter(linkedWIStruct -> isRelevantLinkedWorkItem(linkedWIStruct, linkRoleBackIds))
                       .limit((long) max + 1 - count)
                       .count();
    }
    
    return count < min || count > max;
  }
  
  
  private boolean isRelevantLinkedWorkItem(@NotNull ILinkedWorkItemStruct linkedWIStruct, @NotNull Set<String> linkRoleIds) {
    
    IWorkItem linkedWorkItem = linkedWIStruct.getLinkedItem();
    return linkRoleIds.contains(linkedWIStruct.getLinkRole().getId()) &&
           !linkedWorkItem.isUnresolvable() &&
           (projectId == null || linkedWorkItem.getProjectId().equals(projectId)) &&
           (workItemTargetTypeIds.isEmpty() || workItemTargetTypeIds.contains(linkedWorkItem.getType().getId())) &&
           (workItemTargetStatusIds.isEmpty() || workItemTargetStatusIds.contains(linkedWorkItem.getStatus().getId())) &&
           !(ignoreSuspectLinks && linkedWIStruct.isSuspect());
  }

}

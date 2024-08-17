package com.tulrfsd.polarion.core.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.shared.api.model.wi.WorkItem;
import com.polarion.alm.shared.api.model.wi.linked.LinkedWorkItem;
import com.polarion.alm.shared.api.utils.collections.IterableWithSize;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;

public interface WorkItemCoreHelper {
  
  @Nullable
  public static ILinkedWorkItemStruct getDirectLinkForBackLink(@NotNull IWorkItem origin, @NotNull ILinkedWorkItemStruct backLinkStruct) {
    return backLinkStruct.getLinkedItem()
                         .getLinkedWorkItemsStructsDirect()
                         .stream()
                         .filter(directLinkStruct -> getUniqueId(directLinkStruct.getLinkedItem()).equals(getUniqueId(origin)) &&
                                                     directLinkStruct.getLinkRole().getId().equals(backLinkStruct.getLinkRole().getId()))
                         .findAny()
                         .orElse(null);
  }

  @Nullable
  public static LinkedWorkItem getDirectLinkForBackLink(@NotNull WorkItem origin, @NotNull LinkedWorkItem backWorkItemLink) {
    if (!backWorkItemLink.isBackLink()) {
      return null;
    }
    return backWorkItemLink.fields()
                           .workItem()
                           .get()
                           .fields()
                           .linkedWorkItems()
                           .direct()
                           .toArrayList()
                           .stream()
                           .filter(directWorkItemLink -> getUniqueId(directWorkItemLink.fields().workItem().get()).equals(getUniqueId(origin)) &&
                                                         directWorkItemLink.fields().role().optionId().equals(backWorkItemLink.fields().role().optionId()))
                           .findAny()
                           .orElse(null);
  }

  @NotNull
  public static String getQuery(@NotNull Collection<IWorkItem> workItems) {
    return workItems.stream()
                    .map(WorkItemCoreHelper::getUniqueId)
                    .collect(Collectors.joining(" ", "id:(", ")"));
  }
  
  @NotNull
  public static String getQueryForRenderingAPI(@NotNull Collection<WorkItem> workItems) {
    return workItems.stream()
                    .map(WorkItemCoreHelper::getUniqueId)
                    .collect(Collectors.joining(" ", "id:(", ")"));
  }

  @NotNull
  public static String getUniqueId(@NotNull IWorkItem workItem) {
    return String.format("%s/%s", workItem.getProjectId(), workItem.getId());
  }

  @NotNull
  public static String getUniqueId(@NotNull WorkItem workItem) {
    return String.format("%s/%s", workItem.fields().project().projectId(), workItem.fields().id().get());
  }
  
  @NotNull
  public static List<WorkItem> getDirectLinkedWorkItemsR(@NotNull WorkItem workItem, @Nullable String linkRoleId, @Nullable String typeId) {
    return getLinkedWorkItemsR(workItem.fields().linkedWorkItems().direct(), linkRoleId, typeId);
  }

  @NotNull
  public static List<WorkItem> getDirectLinkedWorkItemsR(@NotNull List<WorkItem> workItems, @Nullable String linkRoleId, @Nullable String typeId) {
    return workItems.stream()
                    .map(workItem -> getDirectLinkedWorkItemsR(workItem, linkRoleId, typeId))
                    .flatMap(List::stream)
                    .distinct()
                    .toList();
  }

  @NotNull
  public static List<WorkItem> getBackLinkedWorkItemsR(@NotNull WorkItem workItem, @Nullable String linkRoleId, @Nullable String typeId) {
    return getLinkedWorkItemsR(workItem.fields().linkedWorkItems().back(), linkRoleId, typeId);
  }
  

  @NotNull
  public static List<WorkItem> getBackLinkedWorkItemsR(@NotNull List<WorkItem> workItems, @Nullable String linkRoleId, @Nullable String typeId) {
    return workItems.stream()
        .map(workItem -> getBackLinkedWorkItemsR(workItem, linkRoleId, typeId))
        .flatMap(List::stream)
        .distinct()
        .toList();
  }
  
  public static List<WorkItem> getLinkedWorkItemsR(IterableWithSize<LinkedWorkItem> linkedWorkItems, String linkRoleId, String typeId) {
    
    return linkedWorkItems.toArrayList()
                          .stream()
                          .filter(linkedWorkItem -> (linkRoleId == null || linkRoleId.isEmpty() || linkRoleId.equals(linkedWorkItem.fields().role().optionId())) &&
                                                     linkedWorkItem.fields().workItem().get() != null &&
                                                    (typeId == null || typeId.isEmpty() || typeId.equals(linkedWorkItem.fields().workItem().get().fields().type().optionId())))
                          .map(linkedWorkItem -> linkedWorkItem.fields().workItem().get())
                          .toList();
  }

  public static List<IWorkItem> getDirectLinkedWorkItems(@NotNull IWorkItem workItem, @Nullable String linkRoleId, @Nullable String typeId) {
    return getLinkedWorkItems(workItem.getLinkedWorkItemsStructsDirect(), linkRoleId, typeId);
  }

  public static List<IWorkItem> getDirectLinkedWorkItems(@NotNull List<IWorkItem> workItems, @Nullable String linkRoleId, @Nullable String typeId) {
    return workItems.stream()
                    .map(workItem -> getDirectLinkedWorkItems(workItem, linkRoleId, typeId))
                    .flatMap(List::stream)
                    .distinct()
                    .toList();
  }
  
  public static List<IWorkItem> getDirectLinkedWorkItemsWithRevision(@NotNull List<IWorkItem> workItems, @Nullable String linkRoleId, @Nullable String typeId) {
    return workItems.stream()
                    .map(workItem -> getDirectLinkedWorkItemsWithRevision(workItem, linkRoleId, typeId))
                    .flatMap(List::stream)
                    .distinct()
                    .toList();
  }
  
  public static List<IWorkItem> getDirectLinkedWorkItemsWithRevision(@NotNull IWorkItem workItem, @Nullable String linkRoleId, @Nullable String typeId) {
    ITrackerService trackerService = ServicesProvider.getTrackerService();
    return  workItem.getLinkedWorkItemsStructsDirect()
                    .stream()
                    .filter(linkStruct -> (linkRoleId == null || linkRoleId.isEmpty() || linkRoleId.equals(linkStruct.getLinkRole().getId()) &&
                                           linkStruct.getLinkedItem() != null))
                    .map(linkStruct -> trackerService.getWorkItemWithRevision(linkStruct.getLinkedItem().getProjectId(), linkStruct.getLinkedItem().getId(), linkStruct.getRevision()))
                    .filter(linkedItem -> linkedItem != null && !linkedItem.isUnresolvable() &&
                                          (typeId == null || typeId.isEmpty() || typeId.equals(linkedItem.getType().getId())))
                    .toList();
  }

  public static List<IWorkItem> getBackLinkedWorkItems(@NotNull IWorkItem workItem, @Nullable String linkRoleId, @Nullable String typeId) {
    return getLinkedWorkItems(workItem.getLinkedWorkItemsStructsBack(), linkRoleId, typeId);
  }

  public static List<IWorkItem> getBackLinkedWorkItems(@NotNull List<IWorkItem> workItems, @Nullable String linkRoleId, @Nullable String typeId) {
    return workItems.stream()
                    .map(workItem -> getBackLinkedWorkItems(workItem, linkRoleId, typeId))
                    .flatMap(List::stream)
                    .distinct()
                    .toList();
  }
  
  public static List<IWorkItem> getLinkedWorkItems(Collection<ILinkedWorkItemStruct> linkStructs, String linkRoleId, String typeId) {
    return linkStructs.stream()
                      .filter(linkStruct -> (linkRoleId == null || linkRoleId.isEmpty() || linkRoleId.equals(linkStruct.getLinkRole().getId()) &&
                                             linkStruct.getLinkedItem() != null && !linkStruct.getLinkedItem().isUnresolvable()) &&
                                            (typeId == null || typeId.isEmpty() || typeId.equals(linkStruct.getLinkedItem().getType().getId())))
                      .map(ILinkedWorkItemStruct::getLinkedItem)
                      .toList();
  }

}

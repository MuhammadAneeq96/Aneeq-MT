package com.tulrfsd.polarion.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.shared.util.StringUtils;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.spi.AbstractObjectEnumeration;
import com.tulrfsd.polarion.core.utils.EnumerationsHelper;
import com.tulrfsd.polarion.core.utils.ServicesProvider;

public class WorkItemEnumeration extends AbstractObjectEnumeration  {
  
  String projectId;
  String query;
  boolean includeId;
  boolean includeProjectName;

  public WorkItemEnumeration(@NotNull String enumId, @Nullable String projectId, @NotNull String query, boolean includeId, boolean includeProjectName) {
    super(enumId);
    this.projectId = projectId;
    this.query = query;
    this.includeId = includeId;
    this.includeProjectName = includeProjectName;
  }

  @Override
  public IEnumOption wrapObject(IPObject object) {
    if (object instanceof IWorkItem workItem) {
      return EnumerationsHelper.wrapWorkItem(super.getEnumId(), workItem, includeId, includeProjectName);
    } 
    throw new IllegalArgumentException("The provided object should have been a work item.");
  }

  @Override
  public List<IEnumOption> getAvailableOptions(Object controlValue, IEnumOption currentValue) {
    List<IEnumOption> options = new ArrayList<>();
    LinkedHashMap<String, IWorkItem> workItems = getWorkItems(query, projectId);
    boolean currentPresent = false;
    
    for (IWorkItem workItem : workItems.values()) {
      if (currentValue != null && EnumerationsHelper.getOptionId(workItem).equals(currentValue.getId())) {
        currentPresent = true; 
      }
      if (workItem.can().read()) {
        options.add(EnumerationsHelper.wrapWorkItem(super.getEnumId(), workItem, includeId, includeProjectName));
      }
      
    } 
    
    if (!currentPresent && currentValue != null) {
      options.add(currentValue); 
    }
    return options;
  }

  @Override
  public IEnumOption wrapOption(String optionId) {
    IWorkItem workItem = EnumerationsHelper.unwrapWorkItemOptionId(optionId);
    if (workItem == null || workItem.isUnresolvable())
      return createPhantomOption(super.getEnumId(), optionId); 
    return EnumerationsHelper.wrapWorkItem(super.getEnumId(), workItem, includeId, includeProjectName);
  }
  
  @SuppressWarnings("unchecked")
  private LinkedHashMap<String, IWorkItem> getWorkItems(@NotNull String query, @Nullable String projectId) {
    IPObjectList<IWorkItem> iPObjectList;
    
    if (projectId != null && !StringUtils.isEmptyTrimmed(projectId)) {
      query = String.format("(%s) AND project.id:(%s)", query, projectId);
    }
    
    if (StringUtils.isEmptyTrimmed(query)) {
      iPObjectList = ServicesProvider.getTrackerService().queryWorkItems("NOT type:heading", "project title");
    } else {
      iPObjectList = ServicesProvider.getTrackerService().queryWorkItems("NOT type:heading AND " + query, "project title");
    } 
    LinkedHashMap<String, IWorkItem> workItemsMap = new LinkedHashMap<>();
    for (IWorkItem workItem : iPObjectList) {
      if (workItem != null && !workItem.isUnresolvable())
        workItemsMap.put(EnumerationsHelper.getOptionId(workItem), workItem); 
    } 
    return workItemsMap;
  }

}

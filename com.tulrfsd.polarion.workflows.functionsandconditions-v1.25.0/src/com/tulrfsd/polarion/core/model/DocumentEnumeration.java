package com.tulrfsd.polarion.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.shared.util.StringUtils;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.spi.AbstractObjectEnumeration;
import com.tulrfsd.polarion.core.utils.EnumerationsHelper;
import com.tulrfsd.polarion.core.utils.ServicesProvider;

public class DocumentEnumeration extends AbstractObjectEnumeration {
  
  String projectId;
  String query;
  boolean includeProjectName;

  public DocumentEnumeration(@NotNull String enumId, @Nullable String projectId, @NotNull String query, boolean includeProjectName) {
    super(enumId);
    this.projectId = projectId;
    this.query = query;
    this.includeProjectName = includeProjectName;
  }

  @Override
  public IEnumOption wrapObject(IPObject object) {
    if (object instanceof IModule module) {
      return EnumerationsHelper.wrapDocument(super.getEnumId(), module, includeProjectName);
    } 
    throw new IllegalArgumentException("The provided object should have been a module (document).");
  }

  @Override
  public List<IEnumOption> getAvailableOptions(Object controlValue, IEnumOption currentValue) {
    List<IEnumOption> options = new ArrayList<>();
    LinkedHashMap<String, IModule> modules = getDocuments(query, projectId);
    boolean currentPresent = false;
    for (IModule module : modules.values()) {
      if (currentValue != null && EnumerationsHelper.getOptionId(module).equals(currentValue.getId())) {
        currentPresent = true; 
      }
      if (module.can().read())
        options.add(EnumerationsHelper.wrapDocument(super.getEnumId(), module, includeProjectName)); 
    } 
    if (!currentPresent && currentValue != null)
      options.add(currentValue); 
    return options;
  }

  @Override
  public IEnumOption wrapOption(String optionId) {
    IModule module = EnumerationsHelper.unwrapModuleOptionId(optionId);
    if (module == null || module.isUnresolvable())
      return createPhantomOption(super.getEnumId(), optionId); 
    return EnumerationsHelper.wrapDocument(super.getEnumId(), module, includeProjectName);
  }
  
  @SuppressWarnings("unchecked")
  private LinkedHashMap<String, IModule> getDocuments(@NotNull String query, @Nullable String projectId) {
    IPObjectList<IModule> iPObjectList;
    
    if (projectId != null && !StringUtils.isEmptyTrimmed(projectId)) {
      query = String.format("(%s) AND project.id:(%s)", query, projectId);
    }
    
    iPObjectList = ServicesProvider.getDataService().searchInstances(IModule.PROTO, query, "project title");

    LinkedHashMap<String, IModule> modulesMap = new LinkedHashMap<>();
    for (IModule module : iPObjectList) {
      if (module != null && !module.isUnresolvable())
        modulesMap.put(EnumerationsHelper.getOptionId(module), module); 
    } 
    return modulesMap;
  }
}

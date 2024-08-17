package com.tulrfsd.polarion.core.utils;

import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.spi.AbstractObjectEnumFactory;
import com.polarion.platform.persistence.spi.EnumOption;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public interface EnumerationsHelper {
  
  public static IEnumOption wrapWorkItem(@NotNull String enumId, @NotNull IWorkItem workItem, boolean includeProjectName) {
    return wrapWorkItem(enumId, workItem, true, includeProjectName);
  }
  
  public static IEnumOption wrapWorkItem(@NotNull String enumId, @NotNull final IWorkItem workItem, boolean includeId, boolean includeProjectName) {
    String workItemUrl = Transaction.getTransaction()
                                    .context()
                                    .createPortalLink()
                                    .project(workItem.getProjectId())
                                    .workItem(workItem.getId())
                                    .toEncodedRelativeUrl();
    
    
    String projectName = ProjectCoreHelper.getProjectNameOrId(workItem.getProject());
    String title = workItem.getTitle();
    if (includeId)
      title = String.format("%s - %s", workItem.getId(), title); 
    if (includeProjectName)
      title = String.format("%s (%s)", title, projectName);
    
    String iconUrl = "";
    if (Objects.nonNull(workItem.getType()) && workItem.getType().getProperties().containsKey("iconURL")) {
      iconUrl = workItem.getType().getProperty("iconURL");
    }
    
    return new EnumOption(enumId, 
        getOptionId(workItem), title, 0, false, 
        AbstractObjectEnumFactory.getExtendedProperties(workItem, iconUrl, workItemUrl));
  }
  
  public static IEnumOption wrapDocument(@NotNull String enumId, @NotNull final IModule module, boolean includeProjectName) {
    String moduleUrl = Transaction.getTransaction()
                                  .context()
                                  .createPortalLink()
                                  .project(module.getProjectId())
                                  .document(module.getModuleFolder(), module.getModuleName())
                                  .toEncodedRelativeUrl();
    
    String projectName = ProjectCoreHelper.getProjectNameOrId(module.getProject());
    String title = String.format("%s / %s", module.getFolder().getTitleOrName(), module.getTitle());
    if (includeProjectName) {
      title = String.format("%s (%s)", title, projectName); 
    }
    
    String iconUrl = "";
    if (Objects.nonNull(module.getType()) && module.getType().getProperties().containsKey("iconURL")) {
      iconUrl = module.getType().getProperty("iconURL");
    }
    
    return new EnumOption(enumId, 
        getOptionId(module), title, 0, false, 
        AbstractObjectEnumFactory.getExtendedProperties(module, iconUrl, moduleUrl));
  }
  
  public static String getOptionId(@NotNull IUniqueObject object) {
    if (object instanceof IModule module) {
      return String.format("%s/%s/%s", object.getProjectId(), module.getModuleFolder(), module.getModuleName());
    } 
    return String.format("%s/%s", object.getProjectId(), object.getId());
  }
  
  public static IWorkItem unwrapWorkItemOptionId(String optionId) {
    return ServicesProvider.getTrackerService().findWorkItem("", optionId);
  }
  
  public static IModule unwrapModuleOptionId(String optionId) {
    String[] splittedOptionId = optionId.split("/");
    if (splittedOptionId.length != 3) {
      return null;
    }
    String projectId = splittedOptionId[0];
    String spaceId = splittedOptionId[1];
    String moduleId = splittedOptionId[2];
    
    return ModuleCoreHelper.getModule(projectId, spaceId, moduleId);
  }
}

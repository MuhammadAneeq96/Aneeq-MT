package com.tulrfsd.polarion.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.location.ILocation;

public interface ModuleCoreHelper {
  
  @Nullable
  public static String getModuleRevision(@Nullable IModule module) {
    if (module == null) {
      return null;
    }
    if (module.getRevision() != null) {
      return module.getRevision();
    }
    
    String revision = module.getLastRevision();
    for (IWorkItem workItem : module.getContainedWorkItems()) {
      String workItemRevision = workItem.getRevision() == null ? workItem.getLastRevision() : workItem.getRevision();
      if (Integer.valueOf(workItemRevision) > Integer.valueOf(revision)) {
        revision = workItemRevision;
      }
    }
    return revision;
  }
  
  @SuppressWarnings("unchecked")
  @NotNull
  public static List<IWorkItem> getInternalWorkItems(@Nullable IModule module, @Nullable String query) {
    if (module == null || query == null) {
      return Collections.emptyList();
    }
    
    String documentQuery;
    if (query.isBlank()) {
      documentQuery = module.createWorkItemsQuery().excludeUnreferenced(true).finish();
    } else {
      documentQuery = module.createWorkItemsQuery().filterBy(query).excludeUnreferenced(true).finish();
    }
    
    IPObjectList<IWorkItem> filteredWorkItems;
    if (module.getRevision() == null) {
      filteredWorkItems = ServicesProvider.getTrackerService().queryWorkItems(documentQuery, null);
    } else {
      filteredWorkItems = ServicesProvider.getTrackerService().queryWorkItemsInBaseline(documentQuery, null, module.getRevision());
    }
    return filteredWorkItems.stream()
                            .filter(workItem -> module.equals((workItem).getModule()) && !(workItem).isUnresolvable())
                            .toList();
  }

  @SuppressWarnings("unchecked")
  @NotNull
  public static List<IWorkItem> getExternalWorkItems(@Nullable IModule module, @Nullable String query) {
    if (module == null || query == null) {
      return Collections.emptyList();
    }
    if (query.isBlank()) {
      return module.getExternalWorkItems();
    }
    
    Map<String, List<IWorkItem>> revisionedMap = new HashMap<>();
    List<IWorkItem> externalWorkItems = module.getExternalWorkItems();
    for (IWorkItem workItem : externalWorkItems) {
      String revision = workItem.getRevision();
      if (revision == null) {
        revision = "";
      }
      revisionedMap.computeIfAbsent(revision,  r -> new ArrayList<>())
                   .add(workItem);
    }    
    
    List<IWorkItem> filteredWorkItems = new ArrayList<>();
    for (Map.Entry<String, List<IWorkItem>> entry : revisionedMap.entrySet()) {
      String idQuery = WorkItemCoreHelper.getQuery(entry.getValue());
      String documentQuery = String.format("(%s) AND %s", query, idQuery);
      if (entry.getKey().isEmpty() && module.getRevision() == null) {
        filteredWorkItems.addAll(ServicesProvider.getTrackerService().queryWorkItems(documentQuery, null));
      } else if (entry.getKey().isEmpty() && module.getRevision() != null) {
        filteredWorkItems.addAll(ServicesProvider.getTrackerService().queryWorkItemsInBaseline(documentQuery, null, module.getRevision()));
      } else if (!entry.getKey().isEmpty()) {
        filteredWorkItems.addAll(ServicesProvider.getTrackerService().queryWorkItemsInBaseline(documentQuery, null, entry.getKey()));
      }
    }
    return filteredWorkItems;
  }
  
  @NotNull
  public static List<IWorkItem> getContainedWorkItems(@Nullable IModule module, @Nullable String query, boolean excludeExternalWorkItems) {
    List<IWorkItem> filteredWorkItems = getInternalWorkItems(module, query);
    if (!excludeExternalWorkItems) {
      filteredWorkItems.addAll(getExternalWorkItems(module, query));
    }
    return filteredWorkItems;
  }
  
  @NotNull
  public static String escapeModuleTitle(@NotNull String title) {
    return title.replaceAll("[^0-9a-zA-Z\\- ]", "_");
  }
  
  @SuppressWarnings("unchecked")
  @Nullable
  public static IModule getSingleModule(@NotNull String query) {
    IPObjectList<IModule> list = ServicesProvider.getDataService().searchInstances(IModule.PROTO, query, "title");
    if (list.isEmpty()) {
      return null;
    } else if (list.size() > 1) {
      Logger.getLogger(CoreLiterals.LOGGER.getValue()).warn(String.format("The query \"%s\" returned more than one document but only one was expected.", query));
    }
    return list.get(0);
  }
  
  @Nullable
  public static Document getSingleDocument(@NotNull String query) {
    IModule module = getSingleModule(query);
    if (module == null) {
      return null;
    } else {
      return Transaction.getTransaction().documents().getBy().oldApiObject(module);
    }
  }
  
  @SuppressWarnings("unchecked")
  @NotNull
  public static List<IModule> getMultipleModules(@NotNull String query) {
    IPObjectList<IModule> list = ServicesProvider.getDataService().searchInstances(IModule.PROTO, query, "title");
    return new ArrayList<>(list);
  }
  
  @SuppressWarnings("unchecked")
  @NotNull
  public static List<Document> getMultipleDocuments(@NotNull String query) {
    List<IModule> modules = getMultipleModules(query);
    return (List<Document>) modules.stream()
                                   .map(module -> Transaction.getTransaction().documents().getBy().oldApiObject(module))
                                   .toList();
  }
  
  @Nullable
  public static IModule getModule(@NotNull String projectId, @NotNull String spaceId, @NotNull String moduleId) {
    ITrackerProject project = ServicesProvider.getTrackerService().getTrackerProject(projectId);
    if (project.isUnresolvable()) {
      return null;
    }
    ILocation projectLocation = project.getLocation();
    ILocation relativeModuleLocation = projectLocation.append(spaceId)
                                                      .append(moduleId)
                                                      .getRelativeLocation(projectLocation);    
    IModule module = ServicesProvider.getTrackerService().getModuleManager().getModule(project, relativeModuleLocation);
    if (module == null || module.isUnresolvable()) {
      return null;
    }
    return module;
  }

}

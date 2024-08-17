package com.tulrfsd.polarion.workflows.documents.functions;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.model.IPObjectList;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class UpdateReferencedWorkItems implements IFunction<IModule> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + UpdateReferencedWorkItems.class.getSimpleName();
  
  IModule module;
  IBaselineCollection collection;
  boolean disregardCollection;

  @Override
  public void execute(ICallContext<IModule> context, IArguments arguments) {

    this.module = context.getTarget();
    this.disregardCollection = arguments.getAsBoolean("disregard.collection", false);
    this.collection = getModuleHeadCollection();
    List<IWorkItem> externalWorkItems = this.module.getExternalWorkItems();
    
    for (IWorkItem workItem : externalWorkItems) {
      if (workItem == null) {
        throw new UserFriendlyRuntimeException("Experienced a null work item.");
      } else if (workItem.isUnresolvable()) {
        throw new UserFriendlyRuntimeException(String.format("The work item %s is not resolvable for the user.", workItem.getId()));
      }
      this.module.freezeExternalWorkItem(workItem, getRevision(workItem));
    }
  }
  
  
  @NotNull
  private String getRevision(@NotNull IWorkItem workItem) {
    if (this.disregardCollection || workItem.getModule() == null || this.collection.getElement(workItem.getModule()) == null) {
      return workItem.getLastRevision();
    }
    
    IBaselineCollectionElement collectionElement = this.collection.getElement(workItem.getModule());
    IModule externalModule = collectionElement.getObjectWithRevision();
    IWorkItem workItemFromCollection = externalModule.getWorkItem(workItem.getId());
    if (workItemFromCollection == null) {
      throw new UserFriendlyRuntimeException("The work item " + workItem.getId() + " does not exist in the document " + externalModule.getTitleOrName() + " for revision " + this.module.getDataRevision() + ". Fix the revision in the collection first, or remove this work item.");
    }
    String revision = workItemFromCollection.getDataRevision();
    if (revision == null) {
      throw new UserFriendlyRuntimeException("The revision of the referenced work item " + workItem.getId() + " in it's document is null.");
    }
    return revision;
  }

  
  @Nullable
  IBaselineCollection getModuleHeadCollection() {
    String locationPath = this.module.getModuleLocation().getLocationPath();
    String collectionQuery = String.format("elements.documents.id:\"%s/%s\"", this.module.getProjectId(), locationPath);
    @SuppressWarnings("unchecked")
    IPObjectList<IBaselineCollection> collectionList = ServicesProvider.getDataService().searchInstances(IBaselineCollection.PROTO, collectionQuery, "id");
    IBaselineCollection localCollection = null;
    for (IBaselineCollection baselineCollection :  collectionList) {
      IBaselineCollectionElement collectionElement = baselineCollection.getElement(this.module);
      if (collectionElement != null && collectionElement.getRevision() == null) {
        // collection contains module in head revision
        localCollection = baselineCollection;
        break;
      }
    }
    return localCollection;
  }
}

package com.tulrfsd.polarion.core.utils;

import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ObjectsFromFieldGetter {
  
  @Nullable
  public static IWorkItem getWorkItemFromEnumField(@NotNull IUniqueObject object, @NotNull String fieldId) {
    
    ParameterValidator.requireNonNull(object, "pObject");
    ParameterValidator.requireNonEmptyString(fieldId, "fieldId");
    
    Object fieldValue = object.getValue(fieldId);
    if (fieldValue != null && fieldValue instanceof IEnumOption enumOption) {
      IPObject fieldObject = object.getDataSvc().getObjectFromEnumOption(enumOption);
      return fieldObject instanceof IWorkItem workItem && !workItem.isUnresolvable()? workItem : null;
    } else {
      return null;
    }
    
  }
  
  @Nullable
  public static IBaselineCollection getCollectionFromEnumField(@NotNull IUniqueObject object, @NotNull String fieldId) {
    
    ParameterValidator.requireNonNull(object, "object");
    ParameterValidator.requireNonNull(fieldId, "fieldId");
    
    Object fieldValue = object.getValue(fieldId);
    if (fieldValue != null && fieldValue instanceof IEnumOption enumOption) {
      IPObject fieldObject = object.getDataSvc().getObjectFromEnumOption(enumOption);
      return fieldObject instanceof IBaselineCollection collection && !collection.isUnresolvable()? collection : null;
    } else {
      return null;
    }
    
  }

  @Nullable
  public static IModule getModuleFromEnumField(@NotNull IUniqueObject object, @NotNull String fieldId) {
    
    ParameterValidator.requireNonNull(object, "object");
    ParameterValidator.requireNonNull(fieldId, "fieldId");
    ParameterValidator.requireNonEmptyString(fieldId, "fieldId");
    
    Object fieldValue = object.getValue(fieldId);
    if (fieldValue != null && fieldValue instanceof IEnumOption enumOption) {
      IPObject fieldObject = object.getDataSvc().getObjectFromEnumOption(enumOption);
      return fieldObject instanceof IModule module && !module.isUnresolvable()? module : null;
    } else {
      return null;
    }

  }

  @Nullable
  public static IModule getModuleFromEnumFieldAndCollection(@NotNull IUniqueObject object, @NotNull String fieldId, @NotNull IBaselineCollection collection) {
    
    ParameterValidator.requireNonNull(object, "pObject");
    ParameterValidator.requireNonEmptyString(fieldId, "fieldId");
    ParameterValidator.requireNonEmptyString(fieldId, "fieldId");
    ParameterValidator.requireNonNull(collection, "collection");
    
    IModule module = getModuleFromEnumField(object, fieldId);
    if (module == null) {
      return null;
    }
    
    IBaselineCollectionElement collectionElement = collection.getElement(module);
    if (collectionElement == null) {
      return null;
    } else {
      return collectionElement.getObjectWithRevision();
    }
    
  }

}

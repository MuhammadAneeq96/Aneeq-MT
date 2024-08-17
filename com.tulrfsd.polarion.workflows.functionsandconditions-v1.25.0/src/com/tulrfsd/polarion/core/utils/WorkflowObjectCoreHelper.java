package com.tulrfsd.polarion.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.shared.util.Optional;
import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowAction;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.spi.AbstractTypedList;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.platform.persistence.spi.EnumOption;
import com.polarion.subterra.base.data.model.IEnumType;
import com.polarion.subterra.base.data.model.IListType;
import com.polarion.subterra.base.data.model.IPrimitiveType;
import com.polarion.subterra.base.data.model.IStructType;
import com.polarion.subterra.base.data.model.IType;

public interface WorkflowObjectCoreHelper {
  
  @NotNull
  public static boolean isFieldDefined(@Nullable IWorkflowObject object, @Nullable String key) {
    return !(object == null || key == null) && (object.getPrototype().getKeyNames().contains(key) || object.getCustomFieldsList().contains(key));
  }
  
  @NotNull
  public static boolean isSingleEnumField(@Nullable IWorkflowObject object, @Nullable String key) {
    return object != null && key != null && object.getFieldType(key) instanceof IEnumType;
  }
  
  @NotNull
  public static boolean isMultiEnumField(@Nullable IWorkflowObject object, @Nullable String key) {
    return object != null && key != null && object.getFieldType(key) instanceof IListType;
  }
  
  @NotNull
  public static boolean isEnumField(@Nullable IWorkflowObject object, @Nullable String key) {
    return isSingleEnumField(object, key) || isMultiEnumField(object, key);
  }
  
  @NotNull
  public static boolean isIntegerField(@Nullable IWorkflowObject object, @Nullable String key) {
    return object != null && key != null && object.getFieldType(key) instanceof IPrimitiveType primitiveType && primitiveType.getTypeName().equals("java.lang.Integer");
  }
  
  @NotNull
  public static boolean isTextField(@Nullable IWorkflowObject object, @Nullable String key) {
    return object != null && key != null && object.getFieldType(key) instanceof IPrimitiveType primitiveType && primitiveType.getTypeName().equals("com.polarion.core.util.types.Text");
  }
  
  @NotNull
  public static boolean isTableField(@Nullable IWorkflowObject object, @Nullable String key) {
    return object != null && key != null && object.getFieldType(key) instanceof IStructType structType && structType.getStructTypeId().equals("Table");
  }
  
  @Nullable
  public static IWorkflowAction getAction(@Nullable IWorkflowAction[] availableActions, @Nullable String actionId, @Nullable String targetStatusId) {
    
    if (availableActions != null && actionId != null && targetStatusId != null) {
      for (IWorkflowAction workflowAction : availableActions) {
        if (workflowAction.getNativeActionId().equals(actionId) && workflowAction.getTargetStatus().getId().equals(targetStatusId)) {
          return workflowAction;
        }
      }
    }
    return null;
  }
  
  public static boolean isActionAvailableForTargetStatus(@Nullable IWorkflowObject object, @Nullable String statusId) {
    
    if (object == null || statusId == null) {
      return false;
    }
    
    for (IWorkflowAction action : object.getAvailableActions()) {
      if (statusId.equals(action.getTargetStatus().getId())) {
        return true;
      }
    }
    return false;
  }
  
  @Nullable
  public static String getRevisionOfLastStatusChange(@Nullable IWorkflowObject object, @Nullable String requiredStatusId) {

    if (object == null || object.isUnresolvable()) {
      return null;
    }

    IPObjectList<IWorkflowObject> objectList = ServicesProvider.getDataService().getObjectHistory(object);
    int size = objectList.size();
    boolean found = false;
    String statusId = objectList.get(size - 1).getStatus().getId();
    String revision = objectList.get(size - 1).getRevision();

    for (int i = size - 1; i >= 0; i--) {
      IWorkflowObject historicObject = objectList.get(i);
      if ((requiredStatusId == null || requiredStatusId.equals(statusId)) &&
          !historicObject.getStatus().getId().equals(statusId)) {
        found = true;
        break;
      }
      revision = historicObject.getRevision();
      statusId = historicObject.getStatus().getId();
    }
    
    if (!found && requiredStatusId != null && !statusId.equals(requiredStatusId)) {
      return null;
    }

    return revision;
  }
  
  public static boolean fieldConditionsPass(IWorkflowObject workflowObject, Map<String, String> regexMap, boolean requireAllPass) {
    
    boolean result = regexMap.isEmpty();
    
    for (Map.Entry<String, String> entry : regexMap.entrySet()) {
      Object fieldValue = workflowObject.getValue(entry.getKey());
      
      result = result || ((fieldValue != null && !(fieldValue instanceof Collection<?> collectionValue && collectionValue.isEmpty())) &&
                           RegExUtils.checkFieldSyntax(workflowObject, entry.getKey(), entry.getValue(), false));
      if (requireAllPass && !result) {
        return false;
      } else if (!requireAllPass && result) {
        return true;
      }
    }
    return result;
  }
  
  @SuppressWarnings("unchecked")
  @NotNull
  public static IPObjectList<IWorkflowObject> getReferencingWorkflowObjects(@NotNull IWorkflowObject workflowObject, @NotNull String prototype, @NotNull String query, boolean projectScope, @NotNull String workflowName) {
    
    if (!Arrays.asList(IWorkItem.PROTO, IModule.PROTO, ITestRun.PROTO).contains(prototype)) {
      throw new UserFriendlyRuntimeException(String.format("Internal error in %s. The method getReferencingWorkflowObjects was handed a wrong prototype.", WorkflowObjectCoreHelper.class.getSimpleName()) );
    }
    query = WorkflowObjectCoreHelper.resolveReferencingQuery(workflowObject, query, projectScope, workflowName);
    return ServicesProvider.getDataService().searchInstances(prototype, escapeHyphen(query), "id");
  }
  
  @Nullable
  public static IBaselineCollection getCollectionFromEnumField(@Nullable IWorkflowObject workflowObject, @Nullable String key) {
    
    if (key == null || key.isEmpty() || workflowObject == null ||
        workflowObject.getValue(key) == null || !WorkflowObjectCoreHelper.isSingleEnumField(workflowObject, key)) {
      return null;
    }
    
    IPObject object = ServicesProvider.getDataService().getObjectFromEnumOption((IEnumOption) workflowObject.getValue(key));
    if ( object != null && object instanceof IBaselineCollection collection && !collection.isUnresolvable()) {
      return collection;
    } else {
      return null;
    }
  }
  
  @NotNull
  public static IModule getModuleFromEnumFieldAndCollection(IWorkflowObject workflowObject, String key, IBaselineCollection collection) {
    
    IPObject pObject = getObjectFromEnumField(workflowObject, key);
    if (!(pObject instanceof IModule)) {
      throw new IllegalArgumentException(String.format("The object in the field %s is not a document.", key));
    }
    
    IModule module = (IModule) pObject;
    IBaselineCollectionElement collectionElement = collection.getElement(module);
    if (collectionElement == null) {
      throw new UserFriendlyRuntimeException("The document \"" + module.getModuleName() + "\" is not contained in the collection \"" + collection.getName() + "\" .");
    }
    
    return collectionElement.getObjectWithRevision();
  }

  @Nullable
  public static IPObject getObjectFromEnumField(@Nullable IWorkflowObject workflowObject, @Nullable String key) {
    
    if (workflowObject == null || key == null || workflowObject.getValue(key) == null) {
      return null;
    } else if (!WorkflowObjectCoreHelper.isSingleEnumField(workflowObject, key)) {
      throw new IllegalArgumentException("The field " + key + " must be an enumeration field. Please check the workflow configuration.");
    }
    
    IEnumOption enumOption = (IEnumOption) workflowObject.getValue(key);
    IPObject object = ServicesProvider.getDataService().getObjectFromEnumOption(enumOption);
    if (object != null && object instanceof IPObject pObject && !object.isUnresolvable()) {
      return pObject;
    } else {
      throw new UserFriendlyRuntimeException(String.format("The %s %s contains the object \"%s\" in the field %s that does not exist or is not resolvable (for the user).",
          workflowObject.getPrototype().getName(), workflowObject.getId(), enumOption.getName(), workflowObject.getFieldLabel(key)));
    }
  }
  
  @NotNull
  public static List<IPObject> getObjectsFromEnumField(IWorkflowObject workflowObject, String key) {
    List<IPObject> objects = new ArrayList<>();
    if (WorkflowObjectCoreHelper.isSingleEnumField(workflowObject, key)) {
      Optional.ofNullable(getObjectFromEnumField(workflowObject, key)).ifPresent(objects::add);
    } else if (WorkflowObjectCoreHelper.isMultiEnumField(workflowObject, key)) {
      CustomTypedList list = (CustomTypedList) workflowObject.getValue(key);
      for (Object enumOption : list) {
        IPObject object = ServicesProvider.getDataService().getObjectFromEnumOption((IEnumOption) enumOption);
        if (object != null && !object.isUnresolvable()) {
          objects.add(object);
        } else {
          throw new UserFriendlyRuntimeException(String.format("The %s %s contains the object \"%s\" in the field %s that does not exist or is not resolvable (for the user).",
              workflowObject.getPrototype().getName(), workflowObject.getId(), ((IEnumOption) enumOption).getName(), workflowObject.getFieldLabel(key)));
        }
      }
    } else {
      throw new IllegalArgumentException("The field " + key + " must be a single or multi enum field. Please check the workflow configuration.");
    }
    return objects;
  }
  
  @Nullable
  private static IType getFieldType(@Nullable IWorkflowObject workflowObject, @Nullable String key) {
    if (workflowObject == null || key == null) {
      return null;
    }
    IType keyType = workflowObject.getPrototype().getKeyType(key);
    if (keyType != null) {
      return keyType;
    }
    
    IType customType = workflowObject.getCustomFieldPrototype(key).getType();
    if (customType != null) {
      return customType;
    }
    
    return null;
  }
  
  @NotNull
  public static List<IUser> getUsersFromFields(@Nullable IWorkflowObject workflowObject, @Nullable Set<String> fieldIds) {
    
    List<IUser> users = new ArrayList<>();
    for (String fieldId : fieldIds) {
      users.addAll(getUsersFromField(workflowObject, fieldId));
    }
    return users;
  }
  
  @SuppressWarnings("unchecked")
  @NotNull
  public static List<IUser> getUsersFromField(@Nullable IWorkflowObject workflowObject, @Nullable String fieldId) {
    
    List<IUser> users = new ArrayList<>();
    
    if (workflowObject == null || fieldId == null || fieldId.isBlank()) {
      return users;
    }
    
    if (workflowObject.getCustomFieldsList().contains(fieldId)) {
      return getUsersFromCustomField(workflowObject, fieldId); 
    }
    
    if (workflowObject instanceof IWorkItem workItem)
      switch (fieldId) {
        case "author":
          return Arrays.asList(workItem.getAuthor());
        case "assignee":
          return workItem.getAssignees();
        case "approvals":
          Collection<IApprovalStruct> approvals = workItem.getApprovals();
          return approvals.stream()
                          .map(IApprovalStruct::getUser)
                          .toList();
        case "watches":
          return workItem.getWatchingUsers();
        default:
          throw new IllegalArgumentException(String.format("The built-in field with the ID %s does not contain users.", fieldId));
      }
    
    if (workflowObject instanceof IModule module) {
      if (fieldId.equals("author")) {
        return Arrays.asList(module.getAuthor());
      } else if (fieldId.equals("updatedBy")) {
        return Arrays.asList(module.getUpdatedBy());
      } else {
        throw new IllegalArgumentException(String.format("The built-in field with the ID %s does not contain users.", fieldId));
      }
    }
    
    if (workflowObject instanceof ITestRun testRun) {
      if (fieldId.equals("author")) {
        return Arrays.asList(testRun.getAuthor());
      } else {
      throw new IllegalArgumentException(String.format("The built-in field with the ID %s does not contain users.", fieldId));
      }
    }
    
    return users;
  }
  
  @SuppressWarnings("unchecked")
  public static List<IUser> getUsersFromCustomField(@Nullable IWorkflowObject workflowObject, @Nullable String fieldId) {
    
    if (workflowObject == null || fieldId == null || fieldId.isBlank() || workflowObject.getValue(fieldId) == null) {
      return Collections.emptyList();
    }
    
    Object fieldValue = workflowObject.getValue(fieldId);
    List<IEnumOption> enumOptions = new ArrayList<>();
    if (fieldValue instanceof IEnumOption enumOption) {
      enumOptions.add(enumOption);
    } else if (fieldValue instanceof CustomTypedList options) {
      enumOptions.addAll(options);
    } else {
      throw new IllegalArgumentException(String.format("The custom field with the ID %s does not contain users.", fieldId));
    }
    
    return enumOptions.stream()
                      .map(enumOption -> ServicesProvider.getProjectService().getUser(enumOption.getId()))
                      .filter(user -> !user.isUnresolvable())
                      .toList();
  }
  
  @Nullable
  public static IWorkflowObject getObjectWithTrueRevision(@Nullable IWorkflowObject workflowObject) {
    return getObjectWithTrueRevision(workflowObject, null);
  }
  
  @Nullable
  public static IWorkflowObject getObjectWithTrueRevision(@Nullable IWorkflowObject workflowObject, @Nullable String revision) {
    
    if (workflowObject == null) {
      return null;
    }
    
    if (revision == null && workflowObject.getRevision() != null) {
      revision = workflowObject.getRevision();
    }
    
    IPObjectList<IWorkflowObject> history =  ServicesProvider.getDataService().getObjectHistory(workflowObject);
    
    for (int idx = history.size(); idx-- > 0; ) {
      IWorkflowObject historicObject = history.get(idx);
      if (revision == null || Integer.valueOf(historicObject.getRevision()) <= Integer.valueOf(revision)) {
        return historicObject;
      }
    }
    return null;
  }
  
  @NotNull
  public static String resolveReferencingQuery(@NotNull IWorkflowObject workflowObject, @NotNull String query, @NotNull boolean projectScope, @NotNull String workflowName) {
    if (query.isBlank()) {
      throw new UserFriendlyRuntimeException(String.format("The query parameter of the %s workflow function/condition must not be empty.", workflowName));
    } else if (projectScope) {
      query = String.format("(%s) AND project.id:(%s)", query, workflowObject.getProjectId());
    }
    
    query = query.replace("{{projectId}}", workflowObject.getProjectId())
                 .replace("{{objectId}}", workflowObject.getId());
    if (IModule.PROTO.equals(workflowObject.getPrototype().getName())) {
      query = query.replace("{{spaceId}}", ((IModule) workflowObject).getModuleFolder());
    }
    
    if (query.contains("{") || query.contains("}")) {
      throw new UserFriendlyRuntimeException(String.format("The query contains wrong placeholders. Please check the %s workflow function/condition configuration. Processed query: %s", workflowName, query));
    }
    
    return query;
  }
  
  @NotNull
  public static String generateName(IWorkflowObject workflowObject, String prefix, String suffix, Set<String> fieldIds, String workflowName) {
    String name = "";
    if (!prefix.isBlank()) {
      name = String.format("%s%s", name, prefix);
    }
    
    for (String fieldId : fieldIds) {
      Object fieldValue = workflowObject.getValue(fieldId);
      if (workflowObject.getFieldType(fieldId) == null) {
        throw new UserFriendlyRuntimeException(String.format("The field with the ID \"%s\" of the %s %s must not be undefined for the workflow function/condition %s.", fieldId, workflowObject.getPrototype().getName(), workflowObject.getId(), workflowName));
      } else if (fieldValue == null || (fieldValue instanceof Collection<?> collectionValue && collectionValue.isEmpty())) {
        throw new UserFriendlyRuntimeException(String.format("The field with the ID \"%s\" of the %s %s must not be empty for the workflow function/condition %s.", fieldId, workflowObject.getPrototype().getName(), workflowObject.getId(), workflowName));
      }
      name = String.format("%s %s", name, FieldRenderer.render(fieldValue));
    }
    
    if (!suffix.isBlank()) {
      name = String.format("%s%s", name, suffix);
    }
    
    if (name.isBlank()) {
      throw new UserFriendlyRuntimeException(String.format("The name generated during the workflow function/condition of the %s %s must not be empty. Please check the %s workflow configuration.", workflowObject.getPrototype().getName(), workflowObject.getId(), workflowName));
    }
    
    return name.trim();
  }
  
  public static void performWorkflowAction(@NotNull IWorkflowObject object, @NotNull String nativeActionId) {
    int actionId = -1;
    for (IWorkflowAction action : object.getAvailableActions()) {
      if (nativeActionId.equals(action.getNativeActionId())) {
        actionId = action.getActionId();
        break;
      }
    }
    if (actionId >= 0) {
      object.performAction(actionId);
      object.save();
    } else {
      throw new IllegalStateException(String.format("Cannot perform the workflow action with the ID %s for the %s %s.", nativeActionId, object.getPrototype().getName(), object.getId()));
    }    
  }
  
  public static  Comparator<IWorkItem> getEnumComparator(List<String> optionIdList, String fieldId) {
    return (workItem1, workItem2) -> {
      EnumOption option1 = (EnumOption) workItem1.getValue(fieldId);
      EnumOption option2 = (EnumOption) workItem2.getValue(fieldId);
      if (option1 == null && option2 == null) {
        return 0;
      } else if(option1 == null) {
        return 1;
      } else if(option2 == null) {
        return -1;
      }
      return Integer.compare(optionIdList.indexOf(option1.getId()), optionIdList.indexOf(option2.getId()));
    };
  }
  
  public static String escapeHyphen(String string) {
    // only escape - when it is not already escaped
    return string.replaceAll("(?<!\\\\)-", "\\\\-");
  }
  
  @Nullable
  public static String getEnumerationOptionId(@Nullable IWorkflowObject workflowObject, @Nullable String fieldId) {
    if (isSingleEnumField(workflowObject, fieldId) && workflowObject.getValue(fieldId) instanceof IEnumOption enumOption) {
      return enumOption.getId();
    }
    return null;
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void setEnumOption(@NotNull IWorkflowObject workflowObject, @NotNull String fieldId, @Nullable IEnumOption enumOption, boolean clearMultiEnum) {
    if (isMultiEnumField(workflowObject, fieldId)) {
      AbstractTypedList multiEnumField = (AbstractTypedList) workflowObject.getValue(fieldId);
      if (clearMultiEnum) {
        multiEnumField.clear();
      }
      if (enumOption != null && !multiEnumField.contains(enumOption)) {
        multiEnumField.add(enumOption);
      }
    } else if (isSingleEnumField(workflowObject, fieldId)) {
      workflowObject.setValue(fieldId, enumOption);
    } else {
      throw new IllegalArgumentException(String.format("The field with the ID %s is not an enum field.", fieldId));
    }
  }
  
  @NotNull
  public static String getPrototypeId(@NotNull String prototypeId) {
    switch (prototypeId.toLowerCase()) {
      case "workitem", "work item":
        prototypeId = IWorkItem.PROTO;
        break;
      case "document", "module":
        prototypeId = IModule.PROTO;
        break;
      case "testrun", "test run":
        prototypeId = ITestRun.PROTO;
        break;
      default:
        throw new IllegalArgumentException(String.format("The prototype \"%s\" parameter in the workflow condition is invalid. Needs to be WorkItem, Module, or TestRun.", prototypeId));
    }
    return prototypeId;
  }

}

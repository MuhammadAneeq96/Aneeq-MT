package com.tulrfsd.polarion.workflows.generic.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.subterra.base.data.model.IEnumType;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ResetFieldValue implements IFunction<IWorkflowObject> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ResetFieldValue.class.getSimpleName();
  
  @Override
  public void execute(ICallContext<IWorkflowObject> context, IArguments arguments) {
    
    List<String> blacklist = new ArrayList<>();
    blacklist.addAll(Arrays.asList("approvals", "author", "autoSuspect", "comments",
        "branchedFrom", "created", "externallyLinkedWorkItems", "hyperlinks", "linkedOslcResources",
        "linkedRevisions", "linkedRevisionsDerived", "linkedWorkItems", "linkedWorkItemsDerived",
        "previousStatus", "updated", "workRecords"));
    
    IWorkflowObject workflowObject = context.getTarget();
    Set<String> fieldIds = arguments.getAsSet("field.ids");
    boolean alwaysClear = arguments.getAsBoolean("always.clear", false);
    
    blacklist.retainAll(Arrays.asList(fieldIds.toArray()));
    if (!blacklist.isEmpty()) {
      throw new UserFriendlyRuntimeException(String.format("The field %s is not supported by the workflow function %s.", workflowObject.getFieldLabel(blacklist.get(0)), FUNCTION_NAME));
    }
    
    if (alwaysClear) {
      clearFields(workflowObject, fieldIds);
    }
    
    for (String fieldId : fieldIds) {
      boolean isEnumField = WorkflowObjectCoreHelper.isEnumField(workflowObject, fieldId);
      boolean isCustomField = workflowObject.getCustomFieldsList().contains(fieldId);
      Object defaultFieldValue = isCustomField ? workflowObject.getCustomFieldPrototype(fieldId).getDefaultValue() : null;
      
      if (isEnumField) {
        setEnumDefaultValue(workflowObject, fieldId);
        if (defaultFieldValue instanceof IEnumOption enumOption) {
          WorkflowObjectCoreHelper.setEnumOption(workflowObject, fieldId, enumOption, true);
        }
      } else {
        workflowObject.setValue(fieldId, defaultFieldValue);
      }
      
    }
  }

  private void clearFields(IWorkflowObject workflowObject, Set<String> fieldIds) {
    for (String fieldId : fieldIds) {
      if (WorkflowObjectCoreHelper.isEnumField(workflowObject, fieldId)) {
        WorkflowObjectCoreHelper.setEnumOption(workflowObject, fieldId, null, true);
      } else {
        workflowObject.setValue(fieldId, null);
      }
    }
  }

  private void setEnumDefaultValue(@NotNull IWorkflowObject workflowObject, @NotNull String fieldId) {
    Optional<IEnumType> enumTypeOptional = workflowObject.getEnumerationTypeForField(fieldId);
    IEnumOption defaultOption;
    if (fieldId.equals("assignee")) {
      defaultOption = null;
    } else if (!enumTypeOptional.isPresent()) {
      throw new UserFriendlyRuntimeException(String.format("The field with the ID %s does not have an enumaration but is supposed to. Check the %s workflow function configuration.", fieldId, FUNCTION_NAME));
    } else {
      IEnumeration<?> enumeration = workflowObject.getDataSvc().getEnumerationForEnumId(enumTypeOptional.get(), workflowObject.getContextId());
      defaultOption = enumeration.getDefaultOption(enumeration.getControlKey());
    }
    WorkflowObjectCoreHelper.setEnumOption(workflowObject, fieldId, defaultOption, true);
  }

}

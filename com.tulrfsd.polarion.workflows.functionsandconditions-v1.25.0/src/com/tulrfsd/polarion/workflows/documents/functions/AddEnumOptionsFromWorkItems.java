package com.tulrfsd.polarion.workflows.documents.functions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.spi.CustomTypedList;
import com.polarion.subterra.base.data.model.IEnumType;
import com.tulrfsd.polarion.core.utils.ModuleCoreHelper;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class AddEnumOptionsFromWorkItems implements IFunction<IModule> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + AddEnumOptionsFromWorkItems.class.getSimpleName();

  @SuppressWarnings("unchecked")
  @Override
  public void execute(ICallContext<IModule> context, IArguments arguments) {
    
    IModule module = context.getTarget();
    String query = arguments.getAsString("query", "NOT type:(heading)");
    final boolean includeInternalWorkItems = arguments.getAsBoolean("include.internal.workitems", true);
    final boolean includeExternalWorkItems = arguments.getAsBoolean("include.external.workitems", true);
    String documentFieldId = arguments.getAsString("document.field.id");
    Set<String> workItemFieldIds = arguments.getAsSet("workitem.field.ids");
    final boolean clearDocumentField = arguments.getAsBoolean("clear.document.field", false);
    boolean onlyValidOptions = arguments.getAsBoolean("only.valid.options", true);
    
    performChecks(module, includeInternalWorkItems, includeExternalWorkItems, documentFieldId);
    
    List<IWorkItem> workItems = new ArrayList<>();
    Set<IEnumOption> enumOptions = new HashSet<>();
    if (includeInternalWorkItems) {
      workItems.addAll(ModuleCoreHelper.getInternalWorkItems(module, query));
    }
    if (includeExternalWorkItems) {
      workItems.addAll(ModuleCoreHelper.getExternalWorkItems(module, query));
    }
    
    for (IWorkItem workItem : workItems) {
      for (String workItemFieldId : workItemFieldIds) {
        processWorkItemField(enumOptions, workItem, workItemFieldId);
      }
    }
    
    if (onlyValidOptions) {
      enumOptions = getWrappedEnumOptions(module, enumOptions, documentFieldId);
    }
    
    CustomTypedList moduleEnumOptions = (CustomTypedList) module.getValue(documentFieldId);
    if (clearDocumentField) {
      moduleEnumOptions.clear();
    }
    enumOptions.removeAll(moduleEnumOptions);
    moduleEnumOptions.addAll(enumOptions);
    
  }

  private void performChecks(IModule module, boolean includeInternalWorkItems, boolean includeExternalWorkItems, String documentFieldId) {
    if (!WorkflowObjectCoreHelper.isFieldDefined(module, documentFieldId)) {
      throw new UserFriendlyRuntimeException(String.format("The document field with the ID \"%s\" is not defined. Check the configuration of the workflow function %s.", documentFieldId, FUNCTION_NAME));
    }
    if (!WorkflowObjectCoreHelper.isMultiEnumField(module, documentFieldId)) {
      throw new UserFriendlyRuntimeException(String.format("The document field with the ID \"%s\" is not a multi-select enum field. Check the configuration of the workflow function %s.", documentFieldId, FUNCTION_NAME));
    }
    if (!includeExternalWorkItems && !includeInternalWorkItems) {
      throw new UserFriendlyRuntimeException(String.format("You cannot exclude the internal and external work items at the same time. Check the configuration of the workflow function %s.", FUNCTION_NAME));
    }
  }

  @SuppressWarnings("unchecked")
  private void processWorkItemField(Set<IEnumOption> enumOptions, IWorkItem workItem, String fieldId) {
    
    if (!WorkflowObjectCoreHelper.isFieldDefined(workItem, fieldId)) {
      return;
    } else if (!WorkflowObjectCoreHelper.isEnumField(workItem, fieldId)) {
      throw new UserFriendlyRuntimeException(String.format("The field with the ID \"%s\" is not an enum field of the work item %s. Check the configuration of the workflow function %s.", fieldId, workItem.getId(), FUNCTION_NAME));
    }
    
    Object fieldValue = workItem.getValue(fieldId);
    if (fieldValue instanceof IEnumOption enumOption) {
      enumOptions.add(enumOption);
    } else if (fieldValue instanceof CustomTypedList options) {
      enumOptions.addAll(options);
    }
  }
  

  @NotNull
  private Set<IEnumOption> getWrappedEnumOptions(IModule module, Set<IEnumOption> enumOptions, String fieldId) {
    Optional<IEnumType> enumTypeOptional = module.getEnumerationTypeForField(fieldId);
    if (!enumTypeOptional.isPresent()) {
      throw new UserFriendlyRuntimeException(String.format("The document field with the ID \"%s\" does not have an enumaration but is supposed to. Check the %s workflow function configuration.", fieldId, FUNCTION_NAME));
    }
    IEnumeration<?> enumeration = module.getDataSvc().getEnumerationForEnumId(enumTypeOptional.get(), module.getContextId());
    return enumOptions.stream()
                      .map(enumOption -> {
                        IEnumOption wrappedEnumOption = enumeration.wrapOption(enumOption.getId(), module);
                        if (wrappedEnumOption.isPhantom()) {
                          throw new UserFriendlyRuntimeException(String.format("The enum option with the ID \"%s\" is not a valid option for the module field %s. Check the configuration of the workflow function %s.", enumOption.getId(), fieldId, FUNCTION_NAME));
                          }
                        return wrappedEnumOption;
                        })
                      .collect(Collectors.toSet());
  }

}

package com.tulrfsd.polarion.workflows.generic.functions;

import java.util.Set;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.spi.AbstractTypedList;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class SetEnumOptions implements IFunction<IWorkflowObject> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + SetEnumOptions.class.getSimpleName();

  @SuppressWarnings("rawtypes")
  @Override
  public void execute(ICallContext<IWorkflowObject> context, IArguments arguments) {
    
    IWorkflowObject workflowObject = context.getTarget();
    String fieldId = arguments.getAsString("field.id");
    Set<String> optionIds = arguments.getAsSet("option.ids");
    boolean clearMultiEnum = arguments.getAsBoolean("clear.multi.enum.field", true);
    boolean onlyValidOptions = arguments.getAsBoolean("only.valid.options", true);
    
    performChecks(workflowObject, fieldId, optionIds);
    
    if (clearMultiEnum && WorkflowObjectCoreHelper.isMultiEnumField(workflowObject, fieldId)) {
      ((AbstractTypedList) workflowObject.getValue(fieldId)).clear();
    }
    
    if (fieldId.equals("assignee")) {
      addAssignee(workflowObject, optionIds);
    } else {
      for (String optionId : optionIds) {
        IEnumOption enumOption = workflowObject.getEnumerationOptionForField(fieldId, optionId);
        if (onlyValidOptions && enumOption.isPhantom()) {
          throw new UserFriendlyRuntimeException(String.format("The enum option with the ID \"%s\" is not a valid option for the field %s. Check the configuration of the workflow function %s.", optionId, workflowObject.getFieldLabel(fieldId), FUNCTION_NAME));
        }
        WorkflowObjectCoreHelper.setEnumOption(workflowObject, fieldId, enumOption, false);
      }
    }
  }

  private void performChecks(IWorkflowObject workflowObject, String fieldId,
      Set<String> optionIds) {
    if (!WorkflowObjectCoreHelper.isEnumField(workflowObject, fieldId)) {
      throw new UserFriendlyRuntimeException(String.format("The field with the ID %s is not an enum field. Check the configuration of the workflow function %s.", fieldId, FUNCTION_NAME));
    }
    if (optionIds.size() > 1 && WorkflowObjectCoreHelper.isSingleEnumField(workflowObject, fieldId)) {
      throw new UserFriendlyRuntimeException(String.format("Cannot store multipe enum options in the single-select enum field with the ID %s. Check the configuration of the workflow function %s.", fieldId, FUNCTION_NAME));
    }
  }

  private void addAssignee(IWorkflowObject workflowObject, Set<String> optionIds) {
    if (!(workflowObject instanceof IWorkItem)) {
      throw new UserFriendlyRuntimeException(String.format("The field Assignee is only available for work items. Check the configuration of the workflow function %s.", FUNCTION_NAME));
    }
    IWorkItem workItem = (IWorkItem) workflowObject;
    IProjectService projectService = ServicesProvider.getProjectService();
    for (String userId : optionIds) {
      IUser user = projectService.getUser(userId);
      if (user.isUnresolvable()) {
        throw new UserFriendlyRuntimeException(String.format("The user with the ID %s is not resolvable. Check the configuration of the workflow function %s.", userId, FUNCTION_NAME));
      }
      workItem.addAssignee(user);
    }
  }
}

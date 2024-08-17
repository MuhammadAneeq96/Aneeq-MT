package com.tulrfsd.polarion.workflows.generic.conditions;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.logging.Logger;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.UserCoreHelper;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class CurrentUserSelectedInField implements ICustomWorkflowCondition<IWorkflowObject> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + CurrentUserSelectedInField.class.getSimpleName();
  static List<String> defaultWorkItemFieldsWithUsers = Arrays.asList("author", "assignee", "approvals", "watches");
  static List<String> defaultDocumentFieldsWithUsers = Arrays.asList("author", "updatedBy" );
  static List<String> defaultTestRunFieldsWithUsers = Arrays.asList("author");
  
  private static final Logger logger = Logger.getLogger(CurrentUserSelectedInField.class);

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    IWorkflowObject workflowObject = context.getTarget();
    final Set<String> fieldIds = arguments.getAsSet("field.ids");
    
    try {
      for (String fieldId : fieldIds) {
        if (passesTypeAndFieldCheck(workflowObject, fieldId) && isUserSelected(workflowObject, fieldId)) {
          return null;
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      throw new UserFriendlyRuntimeException(e);
    }
    
    return "Only users selected in either field of " + getFieldLabels(workflowObject, fieldIds) + " can perform this workflow action.";
  }

  private boolean passesTypeAndFieldCheck(IWorkflowObject workflowObject, String fieldId) {
    return (workflowObject instanceof IWorkItem && defaultWorkItemFieldsWithUsers.contains(fieldId)) ||
           (workflowObject instanceof ITestRun && defaultTestRunFieldsWithUsers.contains(fieldId)) ||
           (workflowObject instanceof IModule && defaultDocumentFieldsWithUsers.contains(fieldId)) ||
            workflowObject.getCustomFieldsList().contains(fieldId);
  }
  
  private boolean isUserSelected(IWorkflowObject workflowObject, String fieldId) {
    return WorkflowObjectCoreHelper.getUsersFromField(workflowObject, fieldId)
                               .contains(UserCoreHelper.getCurrentUser());
  }
  
  private String getFieldLabels(IWorkflowObject workflowObject, Set<String> fieldIds) {
    List<String> fieldList = new ArrayList<>();
    for (String field : fieldIds) {
      String label = workflowObject.getFieldLabel(field);
      if (!label.isEmpty()) {
        fieldList.add(label);
      }
    }
    return String.join(", ", fieldList);
  }
}

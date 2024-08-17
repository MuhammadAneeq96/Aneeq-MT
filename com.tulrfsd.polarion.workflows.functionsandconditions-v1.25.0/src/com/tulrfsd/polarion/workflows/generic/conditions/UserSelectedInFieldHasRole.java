package com.tulrfsd.polarion.workflows.generic.conditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ProjectCoreHelper;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class UserSelectedInFieldHasRole implements ICustomWorkflowCondition<IWorkflowObject> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + UserSelectedInFieldHasRole.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    String roleId = arguments.getAsString("role.id");
    Set<String> fieldIds = arguments.getAsSet("field.ids");
    boolean requireAllHaveRole = arguments.getAsBoolean("require.all.have.role", false);
    boolean requireOneInEachFieldHasRole = arguments.getAsBoolean("require.one.in.each.field.has.role", false);
    boolean allowEmptyFields = arguments.getAsBoolean("allow.empty.fields", true);
    
    IWorkflowObject workflowObject = context.getTarget();
    Collection<IUser> roleUsers = ProjectCoreHelper.getIUsersForRole(workflowObject.getProjectId(), roleId).values();
    boolean found = false;
    
    for (String fieldId : fieldIds) {
      List<IUser> users = new ArrayList<>(WorkflowObjectCoreHelper.getUsersFromField(workflowObject, fieldId));
      boolean foundInField = false;
      int originalSize = users.size();
      users.removeAll(roleUsers);
      
      if (users.size() < originalSize) {
        foundInField = true;
        found = true;
      }
      
      if (requireAllHaveRole && originalSize > 0 && !users.isEmpty()) {
        String userNames = users.stream().map(IUser::getName).collect(Collectors.joining(", "));
        String fieldName = workflowObject.getFieldLabel(fieldId);
        throw new UserFriendlyRuntimeException(String.format("Workflow condition %s did not pass. The user(s) %s selected in the field %s of %s %s do(es) not have the required role %s.",
            CONDITION_NAME, userNames, fieldName, workflowObject.getPrototype().getName(), workflowObject.getId(), roleId));
      }
      
      if (requireOneInEachFieldHasRole && !foundInField && (originalSize > 0 || !allowEmptyFields)) {
        String fieldName = workflowObject.getFieldLabel(fieldId);
        throw new UserFriendlyRuntimeException(String.format("Workflow condition %s did not pass. One of the users in the field %s of %s %s needs to have the role %s.",
            CONDITION_NAME, fieldName, workflowObject.getPrototype().getName(), workflowObject.getId(), roleId));
      }
    }
    
    if (!found) {
      String fieldNames = fieldIds.stream().map(workflowObject::getFieldLabel).collect(Collectors.joining(", "));
      throw new UserFriendlyRuntimeException(String.format("Workflow condition %s did not pass. At least one user selected in the field(s) %s of %s %s needs to have the role %s.",
          CONDITION_NAME, fieldNames, workflowObject.getPrototype().getName(), workflowObject.getId(), roleId));
    }
    
    return null;
  }

}

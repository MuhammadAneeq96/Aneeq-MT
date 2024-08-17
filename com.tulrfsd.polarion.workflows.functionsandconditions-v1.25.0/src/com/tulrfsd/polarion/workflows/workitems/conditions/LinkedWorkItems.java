package com.tulrfsd.polarion.workflows.workitems.conditions;

import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class LinkedWorkItems implements ICustomWorkflowCondition<IWorkItem>{
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + LinkedWorkItems.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IWorkItem> context, IArguments arguments) {
    
    LinkedWorkItemsAction action = new LinkedWorkItemsAction(context, arguments, CONDITION_NAME);
    try {
      return ServicesProvider.getSecurityService().doAsSystemUser(action);
    } catch (UserFriendlyRuntimeException e) {
      return e.getMessage();
    } catch (Exception e) {
      e.printStackTrace();
      return String.format("Error occured while executing the workflow condition %s. Check the logs for details.", CONDITION_NAME);
    }
  }
}

class LinkedWorkItemsAction  implements PrivilegedExceptionAction<String> {
  
  ICallContext<IWorkItem> context;
  IArguments arguments;
  String conditionName;
  IWorkItem workItem;
  Set<String> targetWorkItemTypeIds;
  Set<String> targetWorkItemStatusIds;
  boolean targetTypeOnly;
  boolean targetStatusOnly;
  boolean failForUnresolvableLinks;
  boolean checkRevision;
  
  LinkedWorkItemsAction(ICallContext<IWorkItem> context, IArguments arguments, String conditionName) {
    this.context = context;
    this.arguments = arguments;
    this.conditionName = conditionName;
  }

  @Override
  public String run() throws Exception {
    Set<String> linkRolesDirect = arguments.getAsSetOptional("link.roles.direct");
    Set<String> linkRolesBack = arguments.getAsSetOptional("link.roles.back");
    targetWorkItemTypeIds = arguments.getAsSetOptional("target.workitem.type.ids");
    targetWorkItemStatusIds = arguments.getAsSetOptional("target.workitem.status.ids");
    int min = arguments.getAsInt("min", 1);
    int max = arguments.getAsInt("max", Integer.MAX_VALUE);
    targetTypeOnly = arguments.getAsBoolean("target.type.only", false);
    targetStatusOnly = arguments.getAsBoolean("target.status.only", false);
    failForUnresolvableLinks = arguments.getAsBoolean("fail.for.unresolvable.links", true);
    checkRevision = arguments.getAsBoolean("check.revision", false);
    Map<String, String> regexMap = arguments.getArgumentsWithPrefix("regex.");
    boolean requireAllPass = arguments.getAsBoolean("require.all.regex.pass", false);
    
    if (linkRolesBack.isEmpty() && linkRolesDirect.isEmpty()) {
      return String.format("Either the direct link roles or the back link roles need to be defined in the workflow condition %s.", conditionName);
    }
    
    workItem = context.getTarget();
    int count = 0;
    
    if (!WorkflowObjectCoreHelper.fieldConditionsPass(workItem, regexMap, requireAllPass)) {
      return null;
    }
    
    if (!linkRolesDirect.isEmpty()) {
      count += countLinkedWorkItems(workItem.getLinkedWorkItemsStructsDirect(), linkRolesDirect);
    }
    if (!linkRolesBack.isEmpty()) {
      count += countLinkedWorkItems(workItem.getLinkedWorkItemsStructsBack(), linkRolesBack);
    }
    
    if (count < min) {
      return String.format("The work item %s has %d of the required %d linked work items matching the configuration of the workflow condition %s.", workItem.getId(), count, min, conditionName);
    } else if (count > max) {
      return String.format("The work item %s has %d of the maximum allowed %d linked work items matching the configuration of the workflow condition %s.", workItem.getId(), count, max, conditionName);
    } else {
      return null;
    }
  }
  
  private int countLinkedWorkItems(Collection<ILinkedWorkItemStruct> linkedWorkItemsStructs, Set<String> linkRoleIds) {
    
    int count = 0;
    for (ILinkedWorkItemStruct linkedWIStruct : linkedWorkItemsStructs) {
      IWorkItem linkedWorkItem = linkedWIStruct.getLinkedItem();
      if (linkRoleIds.contains(linkedWIStruct.getLinkRole().getId()) &&
          isValidTarget(linkedWorkItem, linkedWIStruct.getRevision())) {
        count++;
        }
    }
    
    return count;
  }
  
  private boolean isValidTarget(IWorkItem linkedWorkItem, String revision) {
    
    if (checkRevision && revision != null) {
      linkedWorkItem = ServicesProvider.getTrackerService().getWorkItemWithRevision(linkedWorkItem.getProjectId(), linkedWorkItem.getId(), revision);
    }
    
    if (linkedWorkItem.isUnresolvable()) {
      if (failForUnresolvableLinks) {
        throw new UserFriendlyRuntimeException(String.format("The work item %s has the unresolvable linked work item %s%s (detected by worklfow condition %s).", workItem.getId(), linkedWorkItem.getId(), revision != null && checkRevision ? String.format(" for the revision %s", revision) : "", conditionName));
      }
      return false;
    }
    
    if (!targetWorkItemTypeIds.isEmpty() && !targetWorkItemTypeIds.contains(linkedWorkItem.getType().getId())) {
      if (targetTypeOnly) {
        throw new UserFriendlyRuntimeException(String.format("The work item %s has the linked work item %s with a different type than the ones allowed (%s) in the workflow condition %s.", workItem.getId(), linkedWorkItem.getId(), String.join(", ", targetWorkItemTypeIds), conditionName));
      }
      return false;
    }
    
    if (!targetWorkItemStatusIds.isEmpty() && !targetWorkItemStatusIds.contains(linkedWorkItem.getStatus().getId())) {
      if (targetStatusOnly) {
        throw new UserFriendlyRuntimeException(String.format("The work item %s has the linked work item %s with a different status than the ones allowed (%s) in the workflow condition %s.", workItem.getId(), linkedWorkItem.getId(), String.join(", ", targetWorkItemStatusIds), conditionName));
      }
      return false;
    }
    
    return true;
  }
  
}

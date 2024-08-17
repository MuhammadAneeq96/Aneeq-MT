package com.tulrfsd.polarion.workflows.workitems.functions;

import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowAction;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.i18n.Localization;
import com.polarion.platform.security.PermissionDeniedException;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.workflows.utils.Literals;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class ExecuteLinkedWorkItemsAction implements IFunction<IWorkItem> {

  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ExecuteLinkedWorkItemsAction.class.getSimpleName();
  private static final Logger logger = Logger.getLogger(ExecuteLinkedWorkItemsAction.class);
  
  private ICallContext<IWorkItem> context;
  private Set<String> linkRolesDirect;
  private Set<String> linkRolesBack;
  private Set<String> originStates;
  private boolean originNegate;
  private String targetState;
  private Set<String> workItemTypeIds;
  private String actionId;
  private boolean followLinks;
  private IWorkItem baseWorkItem;
  
  public void execute(ICallContext<IWorkItem> context, IArguments arguments) {

    this.context = context;
    linkRolesDirect = arguments.getAsSetOptional("link.roles.direct");
    linkRolesBack = arguments.getAsSetOptional("link.roles.back");
    if (linkRolesDirect.isEmpty() && linkRolesBack.isEmpty()) {
      throw new UserFriendlyRuntimeException(Localization.getString("form.workitem.message.parametersMustBeSet", "link.roles.back", "link.roles.direct"));
    }
    originStates = arguments.getAsSet("origin.states");
    originNegate = arguments.getAsBoolean("origin.negate", false);
    targetState = arguments.getAsString("target.state");
    workItemTypeIds = arguments.getAsSetOptional("workitem.type.ids");
    actionId = arguments.getAsString("action.id");
    followLinks = arguments.getAsBoolean("follow.links", false);

    baseWorkItem = context.getTarget();
    List<IWorkItem> workItems;
    try {
      workItems = ServicesProvider.getSecurityService().doAsSystemUser((PrivilegedExceptionAction<List<IWorkItem>>) this::getWorkItemsToUpdate);
    } catch (Exception e) {
      e.printStackTrace();
      throw new UserFriendlyRuntimeException(String.format("Error occured while executing the workflow function %s. Check the logs for details.", FUNCTION_NAME));
    }
    updateWorkItems(workItems);

  }

  private List<IWorkItem> getWorkItemsToUpdate() {
    Set<IWorkItem> linkedWorkItems = new HashSet<>();
    
    IWorkItem.ITerminalCondition condition = wi -> baseWorkItem.equals(wi);
    
    boolean loopDetected = baseWorkItem.traverseLinkedWorkitems(linkedWorkItems, linkRolesDirect, linkRolesBack, condition);
    if(loopDetected) {
      throw new UserFriendlyRuntimeException("A possible linked work item loop was detected for work item " + baseWorkItem.getId() + ". The operation is canceled to prevent an infinite loop crashing the server.");
    }
    
    if (!followLinks) {
      linkedWorkItems.clear();
      linkedWorkItems.addAll(getImmediateLinks());
    }
    
    linkedWorkItems.remove(baseWorkItem);
    return linkedWorkItems.stream()
                          .filter(workItem -> !workItem.isUnresolvable() &&
                                              (workItemTypeIds.isEmpty() || workItemTypeIds.contains(workItem.getType().getId())) &&
                                               workItem.getStatus() != null &&
                                              (originStates.contains(workItem.getStatus().getId()) ^ originNegate) && // ^ is XOR operator
                                              !targetState.equals(workItem.getStatus().getId()))
                          .toList();
  }

  private void updateWorkItems(List<IWorkItem> workItemsToUpdate) {
    
    String cannotExecuteBecauseCannotChangeLinkedMessage = Localization.getString("form.workitem.message.cannotExecuteBecauseCannotChangeLinked");
    
    for (IWorkItem wi : workItemsToUpdate) {
      if (!wi.can().modifyKey("status")) {
        throw new UserFriendlyRuntimeException(cannotExecuteBecauseCannotChangeLinkedMessage + " You do not have the permissions to modify the status of the work item " + wi.getId() + " in the project with the ID \"" + wi.getProjectId() + "\".");
      }
      IWorkflowAction[] availableActions = wi.getAvailableActions();
      boolean actionAvailable = false;
      for (IWorkflowAction workflowAction : availableActions) {
        if (workflowAction.getNativeActionId().equals(actionId) && workflowAction.getTargetStatus().getId().equals(targetState)) {
          try {
            IWorkItem preparedWorkItem = this.context.prepareObjectForModification(wi);
            preparedWorkItem.performAction(workflowAction.getActionId());
            preparedWorkItem.save();
          } catch (PermissionDeniedException e) {
            logger.warn(e.getMessage(), e);
            throw new UserFriendlyRuntimeException(cannotExecuteBecauseCannotChangeLinkedMessage);
          }
          actionAvailable = true;
          break;
        }
      }
      if (!actionAvailable) {
        throw new UserFriendlyRuntimeException(cannotExecuteBecauseCannotChangeLinkedMessage + " The action " + actionId + " is not available for work item " + wi.getId() + " in the project " + wi.getProject().getName() + ".");
      }
    }
  }
  
  @NotNull
  private List<IWorkItem> getImmediateLinks() {
    List<IWorkItem> linkedWorkItems = new ArrayList<>();
    
    linkedWorkItems.addAll(baseWorkItem.getLinkedWorkItemsStructsDirect().stream()
        .filter(linkedWorkItemStruct -> linkRolesDirect.contains(linkedWorkItemStruct.getLinkRole().getId()))
        .map(ILinkedWorkItemStruct::getLinkedItem)
        .toList());
    
    linkedWorkItems.addAll(baseWorkItem.getLinkedWorkItemsStructsBack().stream()
        .filter(linkedWorkItemStruct -> linkRolesBack.contains(linkedWorkItemStruct.getLinkRole().getId()))
        .map(ILinkedWorkItemStruct::getLinkedItem)
        .toList());
    
    return linkedWorkItems;
  }
}

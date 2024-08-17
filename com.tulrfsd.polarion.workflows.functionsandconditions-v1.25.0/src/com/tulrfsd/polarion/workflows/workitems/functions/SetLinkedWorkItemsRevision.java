package com.tulrfsd.polarion.workflows.workitems.functions;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.types.DateOnly;
import com.polarion.platform.persistence.IEnumOption;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class SetLinkedWorkItemsRevision implements IFunction<IWorkItem> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + SetLinkedWorkItemsRevision.class.getSimpleName();
  
  IWorkItem workItem;

  @Override
  public void execute(ICallContext<IWorkItem> context, IArguments arguments) {
    
    Set<String> linkRoleDirectIds = arguments.getAsSetOptional("link.role.direct.ids");
    Set<String> workItemTargetTypeIds = arguments.getAsSetOptional("workitem.target.type.ids");
    Set<String> workItemTargetStatusIds = arguments.getAsSetOptional("workitem.target.status.ids");
    String dateFieldId = arguments.getAsString("date.field.id", null);
    boolean alwaysUseRevisionFromDate = arguments.getAsBoolean("always.use.revision.from.date", false);
    boolean useLastStatusTransition = arguments.getAsBoolean("use.last.status.transition", false);
    boolean requireStatusTransition = arguments.getAsBoolean("require.status.transition", false);
    String transitionStatusId = arguments.getAsString("transition.status.id", null);
    
    this.workItem = context.getTarget();
    
    performChecks(alwaysUseRevisionFromDate, useLastStatusTransition, dateFieldId);
    List<ILinkedWorkItemStruct> linkedWorkItemStructs = getDirectLinkedWorkItemStructs(linkRoleDirectIds, workItemTargetTypeIds, workItemTargetStatusIds);
    
    if (useLastStatusTransition) {
      setLinkRevisionsFromStatusTransition(linkedWorkItemStructs, transitionStatusId, requireStatusTransition);
    } else {
      setLinkRevisionsFromDate(linkedWorkItemStructs, dateFieldId);
    }
  }
  
  private void performChecks(boolean alwaysUseRevisionFromDate, boolean useLastStatusTransition, String dateFieldId) {
    if (alwaysUseRevisionFromDate && useLastStatusTransition) {
      throw new IllegalArgumentException(String.format("The workflow parameters always.use.revision.from.date and use.last.status.transition cannot be used at the same time. Please check the workflow function configuration of %s.", FUNCTION_NAME));
    }
    if (alwaysUseRevisionFromDate && dateFieldId == null) {
      throw new IllegalArgumentException(String.format("The date field ID needs to be provided to the workflow function %s when the revision shall always be based on the date in the field.", FUNCTION_NAME));
    }
    
    if (alwaysUseRevisionFromDate && getDate(dateFieldId) == null) {
      throw new UserFriendlyRuntimeException(String.format("The field with the id \"%s\" is not defined or empty. Cannot use the date for the workflow function %s.", dateFieldId, FUNCTION_NAME));
    }
  }
  
  @NotNull
  private List<ILinkedWorkItemStruct> getDirectLinkedWorkItemStructs(Collection<String> linkRoleDirectIds, Collection<String> workItemTargetTypeIds, Collection<String> workItemTargetStatusIds) {
    return workItem.getLinkedWorkItemsStructsDirect()
                   .stream()
                   .filter(struct -> (linkRoleDirectIds.isEmpty() || linkRoleDirectIds.contains(struct.getLinkRole().getId())) &&
                                     (struct.getLinkedItem() != null && !struct.getLinkedItem().isUnresolvable()) &&
                                     (workItemTargetTypeIds.isEmpty() || workItemTargetTypeIds.contains(struct.getLinkedItem().getType().getId())) &&
                                     (workItemTargetStatusIds.isEmpty() || workItemTargetStatusIds.contains(struct.getLinkedItem().getStatus().getId())))
                   .toList();
  }

  @Nullable
  private Date getDate(@Nullable String dateFieldId) {
    if (dateFieldId == null) {
      return null;
    }
    if (workItem.getValue(dateFieldId) instanceof Date dateField) {
      return dateField;
    } else if (workItem.getValue(dateFieldId) instanceof DateOnly dateOnlyField) {
      return dateOnlyField.getDate();
    }
    return null;
  }

  private void setLinkRevisionsFromStatusTransition(@NotNull List<ILinkedWorkItemStruct> linkedWorkItemStructs, @Nullable String transitionStatusId, boolean requireStatusTransition) {
    for (ILinkedWorkItemStruct struct : linkedWorkItemStructs) {
      String revision = WorkflowObjectCoreHelper.getRevisionOfLastStatusChange(struct.getLinkedItem(), transitionStatusId);
      if (revision == null && requireStatusTransition) {
        IEnumOption statusOpt = struct.getLinkedItem().getEnumerationOptionForField("status", transitionStatusId);
        if (statusOpt.isPhantom()) {
          throw new UserFriendlyRuntimeException(String.format("The workflow function %s failed. The status with the ID \"%s\" is not defined for the work item %s.", FUNCTION_NAME, transitionStatusId, struct.getLinkedItem().getId()));
        } else {
          throw new UserFriendlyRuntimeException(String.format("The workflow function %s failed. The work item %s did not yet transition to the status \"%s\".", FUNCTION_NAME, struct.getLinkedItem().getId(), statusOpt.getName()));
        }
      }
      struct.setRevision(revision);
    }
  }
  
  private void setLinkRevisionsFromDate(@NotNull List<ILinkedWorkItemStruct> linkedWorkItemStructs, @NotNull String dateFieldId) {
    Date date = getDate(dateFieldId);
    for (ILinkedWorkItemStruct struct : linkedWorkItemStructs) {
      if (date == null) {
        struct.setRevision(struct.getLinkedItem().getLastRevision());
        continue;
      }
      String dateRevision = workItem.getDataSvc().getStorageRevisionAt(date).getName();
      IWorkItem historicWorkItem = (IWorkItem) WorkflowObjectCoreHelper.getObjectWithTrueRevision(struct.getLinkedItem(), dateRevision);
      if (historicWorkItem == null) {
        throw new UserFriendlyRuntimeException(String.format("The workflow function %s failed. The work item %s did not yet exist at %s.", FUNCTION_NAME, struct.getLinkedItem().getId(), date));
      }
      struct.setRevision(historicWorkItem.getRevision());
    }
  }
}

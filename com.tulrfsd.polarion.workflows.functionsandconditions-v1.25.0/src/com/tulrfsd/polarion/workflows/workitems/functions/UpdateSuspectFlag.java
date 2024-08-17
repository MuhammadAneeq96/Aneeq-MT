package com.tulrfsd.polarion.workflows.workitems.functions;

import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.security.PermissionDeniedException;
import com.polarion.platform.spi.security.IAuthenticationSource;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.workflows.utils.Literals;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Set;

public class UpdateSuspectFlag implements IFunction<IWorkItem> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + UpdateSuspectFlag.class.getSimpleName();
  static final IAuthenticationSource authSource = PlatformContext.getPlatform().lookupService(IAuthenticationSource.class);
  
  ICallContext<IWorkItem> context;
  IWorkItem workItem;
  Set<String> workItemTypeIds;
  Set<String> linkRolesDirect;
  Set<String> linkRolesBack;
  boolean setSuspect;  
  List<ILinkedWorkItemStruct> outboundLinks;
  List<ILinkedWorkItemStruct> inboundLinks;
  String userId;

  public void execute(ICallContext<IWorkItem> context, IArguments arguments) {
    this.context = context;
    workItem = context.getTarget();
    this.workItemTypeIds = arguments.getAsSetOptional("workitem.type.ids");
    this.linkRolesDirect = arguments.getAsSetOptional("link.roles.direct");
    this.linkRolesBack = arguments.getAsSetOptional("link.roles.back");
    this.setSuspect = arguments.getAsBoolean("set.suspect", true);
    final boolean useSystemUser = arguments.getAsBoolean("use.system.user", false);
    userId = ServicesProvider.getProjectService().getCurrentUser().getId();
    
    try {
      ISecurityService securityService = ServicesProvider.getSecurityService();
      securityService.doAsSystemUser((PrivilegedExceptionAction<Void>) this::getOutboundLinks);
      securityService.doAsSystemUser((PrivilegedExceptionAction<Void>) this::getInboundLinks);
      if (useSystemUser) {
        securityService.doAsSystemUser((PrivilegedExceptionAction<Void>) this::updateOutboundSuspectFlag);
        securityService.doAsSystemUser((PrivilegedExceptionAction<Void>) this::updateInboundSuspectFlag);
      } else {
        updateOutboundSuspectFlag();
        updateInboundSuspectFlag();
      }
    } catch (UserFriendlyRuntimeException e) {
      throw new UserFriendlyRuntimeException(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      throw new UserFriendlyRuntimeException(String.format("Error occured while executing the workflow function %s. Check the logs for details.", FUNCTION_NAME));
    }

  }
  
  private Void getOutboundLinks() {
    outboundLinks = workItem.getLinkedWorkItemsStructsDirect()
                            .stream()
                            .filter(linkedWorkItemStruct -> !linkedWorkItemStruct.getLinkedItem().isUnresolvable() &&
                                                            (linkRolesDirect.isEmpty() || linkRolesDirect.contains(linkedWorkItemStruct.getLinkRole().getId())) &&
                                                            (workItemTypeIds.isEmpty() || workItemTypeIds.contains(linkedWorkItemStruct.getLinkedItem().getType().getId())) &&
                                                            linkedWorkItemStruct.isSuspect() != setSuspect)
                            .toList();
    return null;
  }

  private Void getInboundLinks() {
    inboundLinks = workItem.getLinkedWorkItemsStructsBack()
                   .stream()
                   .filter(linkedWorkItemStruct -> !linkedWorkItemStruct.getLinkedItem().isUnresolvable() &&
                                                   (linkRolesBack.isEmpty() || linkRolesBack.contains(linkedWorkItemStruct.getLinkRole().getId())) &&
                                                   (workItemTypeIds.isEmpty() || workItemTypeIds.contains(linkedWorkItemStruct.getLinkedItem().getType().getId())) &&
                                                   linkedWorkItemStruct.isSuspect() != setSuspect)
                   .toList();
    return null;
  }
  
  private Void updateOutboundSuspectFlag() {
    for (ILinkedWorkItemStruct linkedWorkItemStruct : outboundLinks) {
      linkedWorkItemStruct.setSuspect(setSuspect);
    }
    try {
      workItem.save();
    } catch (PermissionDeniedException e) {
      throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to modify the work item links of %s.", FUNCTION_NAME, workItem.getId()));
    }
    return null;
  }
  
  private Void updateInboundSuspectFlag() {
    for (ILinkedWorkItemStruct linkedWorkItemStruct : inboundLinks) {
      IWorkItem linkedWorkItem = linkedWorkItemStruct.getLinkedItem();
      if (linkedWorkItem.isUnresolvable()) {
        throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to access the linked work item %s.", FUNCTION_NAME, linkedWorkItem.getId()));
      } else if (!linkedWorkItem.can().modifyKey("linkedWorkItems") ||
                 !authSource.getAuthSrcPermission(linkedWorkItem.getLocation().getLocationPath(), userId).canWrite()) {
        throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to modify the work item links of the linked work item %s (%s).", FUNCTION_NAME, linkedWorkItem.getId(), linkedWorkItem.getProject().getName()));
      }
      IWorkItem preparedWorkItem = this.context.prepareObjectForModification(linkedWorkItem);
      updateSuspectFlag(preparedWorkItem, workItem, linkedWorkItemStruct.getLinkRole());
    }
    return null;
  }
  
  private void updateSuspectFlag(IWorkItem sourceWorkItem, IWorkItem targetWorkItem, ILinkRoleOpt linkRoleOpt) {
    
    for (ILinkedWorkItemStruct linkedWorkItemStruct : sourceWorkItem.getLinkedWorkItemsStructsDirect()) {
      if (!linkedWorkItemStruct.getLinkedItem().equals(targetWorkItem) || !linkedWorkItemStruct.getLinkRole().equals(linkRoleOpt)) {
        continue;
      }
      try {
        linkedWorkItemStruct.setSuspect(setSuspect);
        sourceWorkItem.save();
      } catch (PermissionDeniedException e) {
        throw new UserFriendlyRuntimeException(String.format("Error in %s workflow function: The current user does not have the permission to modify the work item links of the linked work item %s (%s).", FUNCTION_NAME, sourceWorkItem.getId(), sourceWorkItem.getProject().getName()));
      }
    }
  }
}

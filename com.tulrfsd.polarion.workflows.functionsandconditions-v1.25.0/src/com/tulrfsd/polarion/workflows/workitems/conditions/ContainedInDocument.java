package com.tulrfsd.polarion.workflows.workitems.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.IEnumOption;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ContainedInDocument implements ICustomWorkflowCondition<IWorkItem>{
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ContainedInDocument.class.getSimpleName();
  
  Set<String> moduleTypeIds;
  Set<String> moduleStatusIds;
  boolean negate;
  IWorkItem workItem;
  IModule module;

  @Override
  public String checkCondition(ICallContext<IWorkItem> context, IArguments arguments) {
    moduleTypeIds = arguments.getAsSetOptional("module.type.ids");
    moduleStatusIds = arguments.getAsSetOptional("module.status.ids");
    negate = arguments.getAsBoolean("negate", false);
    workItem = context.getTarget();
    module = workItem.getModule();
    
    try {
      if (!negate && (module == null || module.isUnresolvable())) {
        return "Problem: The work item " + workItem.getId() + " is not contained in a resolvable document.";
      } else if (negate && !(module == null || module.isUnresolvable()) && moduleStatusIds.isEmpty() && moduleTypeIds.isEmpty()) {
        return "Problem: The work item " + workItem.getId() + " is contained in a document.";
      }
      
      String statusReturnMessage = checkDocumentStatus();
      if (statusReturnMessage != null) {
        return statusReturnMessage;
      }
      
      String typeReturnMessage = checkDocumentType();
      if (typeReturnMessage != null) {
        return typeReturnMessage;
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new UserFriendlyRuntimeException(e);
    }
    
    return null;
  }

  
  private String checkDocumentStatus() {
    
    if (moduleStatusIds.isEmpty()) {
      return null;
    }
    
    IStatusOpt currentStatusOpt = module.getStatus();
    String currentStatusName = currentStatusOpt.isPhantom() ? currentStatusOpt.getId() : currentStatusOpt.getName();
    
    if (negate && moduleStatusIds.contains(currentStatusOpt.getId())) {
      return String.format("Problem: The work item %s is contained in a document with the incorrect status: %s.", workItem.getId(), currentStatusName);
    } else if (!negate && !moduleStatusIds.contains(currentStatusOpt.getId())) {
      List<String> validStatuses = new ArrayList<>();
      for (String statusId : moduleStatusIds) {
        IEnumOption statusEnumOpt = module.getEnumerationOptionForField("status", statusId);
        String statusName = statusEnumOpt.isPhantom() ? statusEnumOpt.getId() : statusEnumOpt.getName();
        validStatuses.add(statusName);
      }
      return String.format("Problem: The work item %s is contained in a document with the incorrect status: %s. Allowed statuses: %s.", workItem.getId(), currentStatusName, String.join(", ", validStatuses));
    }
    
    return null;
  }
  
  
  private String checkDocumentType() {
    
    if (moduleTypeIds.isEmpty()) {
      return null;
    }
    
    ITypeOpt currentTypeOpt = module.getType();
    String currentTypeName = currentTypeOpt.isPhantom() ? currentTypeOpt.getId() : currentTypeOpt.getName();
    
    if (negate && moduleTypeIds.contains(currentTypeOpt.getId())) {
      return String.format("Problem: The work item %s is contained in a document of the incorrect type: %s.", workItem.getId(), currentTypeName);
    } else if (!negate && !moduleTypeIds.contains(currentTypeOpt.getId())) {
      List<String> validTypes = new ArrayList<>();
      for (String typeId : moduleTypeIds) {
        IEnumOption typeEnumOpt = module.getEnumerationOptionForField("type", typeId);
        String typeName = typeEnumOpt.isPhantom() ? typeEnumOpt.getId() : typeEnumOpt.getName();
        validTypes.add(typeName);
      }
      return String.format("Problem: The work item %s is contained in a document of the incorrect type: %s. Allowed types: %s.", workItem.getId(), currentTypeName, String.join(", ", validTypes));
    }
    
    return null;
  }

}

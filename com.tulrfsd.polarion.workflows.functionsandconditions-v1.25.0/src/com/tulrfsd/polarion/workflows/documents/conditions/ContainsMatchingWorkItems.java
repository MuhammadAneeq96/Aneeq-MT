package com.tulrfsd.polarion.workflows.documents.conditions;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.platform.persistence.WrapperException;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.ModuleCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ContainsMatchingWorkItems implements ICustomWorkflowCondition<IModule> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ContainsMatchingWorkItems.class.getSimpleName();

  public String checkCondition(ICallContext<IModule> context, IArguments arguments) {
    
    IModule module = context.getTarget();
    String query = arguments.getAsString("query");
    boolean excludeReferencedItems = arguments.getAsBoolean("exclude.referenced.items", false);
    boolean exclusivelyReferencedItems = arguments.getAsBoolean("exclusively.referenced.items", false);
    if (excludeReferencedItems && exclusivelyReferencedItems) {
      throw new IllegalArgumentException(String.format("The arguments exclude.referenced.items and exclusively.referenced.items must not be true at the same time for the document workflow condition %s.", CONDITION_NAME));
    }
    int minimum = arguments.getAsInt("minimum", 1);
    int maximum = arguments.getAsInt("maximum", Integer.MAX_VALUE);
    
    if (isModuleNew(module)) {
      return String.format("The workflow condition %s does not properly work during the initialization of documents. Please remove it from the init action.", CONDITION_NAME);
    }
    
    if (module.isModified()) {
      return String.format("All work item changes need to be saved before the workflow condition %s can be executed. Please save the document first without performing the workflow action.", CONDITION_NAME);
    }
        
    int internalCount = ModuleCoreHelper.getInternalWorkItems(module, query).size();
    int externalCount = ModuleCoreHelper.getExternalWorkItems(module, query).size();
    
    int totalCount;
    String filter;
    if (exclusivelyReferencedItems) {
      totalCount = externalCount;
      filter = "external";
    } else if (excludeReferencedItems) {
      totalCount = internalCount;
      filter = "internal";
    } else {
      totalCount = internalCount + externalCount;
      filter = "internal and external";
    }

    if (totalCount < minimum) {
      return String.format("The document only contains %d of the required %d %s work items matching the query %s.", totalCount, minimum, filter, query);
    }
    if (totalCount > maximum) {
      return String.format("The document contains %d which is more than the maximum allowed %d %s work items matching the query %s.", totalCount, maximum, filter, query);
    }
    
    return null;
  }

  private boolean isModuleNew(IModule module) {
    try {
      module.getLastRevision();
      return false;
    } catch (WrapperException e) {
      return true;
    }
  }

}

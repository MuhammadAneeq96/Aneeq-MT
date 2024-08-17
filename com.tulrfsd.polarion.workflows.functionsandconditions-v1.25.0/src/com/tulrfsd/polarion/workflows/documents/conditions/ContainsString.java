package com.tulrfsd.polarion.workflows.documents.conditions;

import java.util.Set;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class ContainsString implements ICustomWorkflowCondition<IModule> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + ContainsString.class.getSimpleName();

  @Override
  public String checkCondition(ICallContext<IModule> context, IArguments arguments) {
    
    IModule module = context.getTarget();
    Set<String> strings = arguments.getAsSet("strings");
    boolean requireAll = arguments.getAsBoolean("require.all", false);
    boolean block = arguments.getAsBoolean("block", false);
    String message = arguments.getAsString("message", "");
    
    String content = module.getHomePageContent().getContent();
    
    for (String string : strings) {
      if (content.contains(string)) {
        if (!requireAll) {
          return block ? "The document contains the forbidden string " + string + " " + message : null;
        }
      } else {
        if (requireAll) {
          return block ? null : "The required string " + string + " is not contained in the document." + " " + message;
        }
      }
    }
    
    if (requireAll) {
      return block ? "All of the following forbidden strings are contained in the document " + strings.toString()  + " " + message : null;
    } else {
      return block ? null : "Not one of the following strings is contained in the document: " + strings.toString() + " " + message;
    }
    
  }

}

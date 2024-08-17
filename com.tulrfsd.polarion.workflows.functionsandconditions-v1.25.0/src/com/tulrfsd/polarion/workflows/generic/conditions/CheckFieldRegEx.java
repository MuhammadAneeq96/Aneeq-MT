package com.tulrfsd.polarion.workflows.generic.conditions;

import java.util.Map;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.core.utils.RegExUtils;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class CheckFieldRegEx implements ICustomWorkflowCondition<IWorkflowObject> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + CheckFieldRegEx.class.getSimpleName();
  private static final String REGEX_PREFIX = "regex.";
  private static final String MESSAGE_PREFIX = "message.";
  private static final String REQUIRE_ALL_ENUM_OPTIONS_PASS = "require.all.enum.options.pass";

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {

    final Map<String, String> fieldRegExMap = arguments.getArgumentsWithPrefix(REGEX_PREFIX);
    final Map<String, String> fieldMessageMap = arguments.getArgumentsWithPrefix(MESSAGE_PREFIX);
    final boolean requireAllEnumOptionsPass = arguments.getAsBoolean(REQUIRE_ALL_ENUM_OPTIONS_PASS, false);
    
    IWorkflowObject workflowObject = context.getTarget();
    
    for (Map.Entry<String, String> entry : fieldRegExMap.entrySet()) {
      String key = entry.getKey();
      String regEx = entry.getValue();
      
      if (RegExUtils.checkFieldSyntax(workflowObject, key, regEx, requireAllEnumOptionsPass)) {
        continue;
      }
      
      // display message instead of regEx when message is available
      if (fieldMessageMap.containsKey(key)) {
        return "Syntax error for field " + workflowObject.getFieldLabel(key) + " of work item " + workflowObject.getId() + ":\n" + fieldMessageMap.get(key);
      } else {
        return "The field " + workflowObject.getFieldLabel(key) + " of work item " + workflowObject.getId() + " does not match the RegEx " + regEx;
      }
    }
    
    return null;
  }



}

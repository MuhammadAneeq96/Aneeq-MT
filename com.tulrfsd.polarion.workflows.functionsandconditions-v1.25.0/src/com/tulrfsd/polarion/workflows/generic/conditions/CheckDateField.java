package com.tulrfsd.polarion.workflows.generic.conditions;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.types.DateOnly;
import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class CheckDateField implements ICustomWorkflowCondition<IWorkflowObject> {
  
  static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + CheckDateField.class.getSimpleName();

  IWorkflowObject workflowObject;
  String fieldId = null;
  String dateIs = null;
  boolean includeCurrentDate;

  @Override
  public String checkCondition(ICallContext<IWorkflowObject> context, IArguments arguments) {
    this.workflowObject = context.getTarget();
    this.fieldId = arguments.getAsString("field.id");
    this.dateIs = arguments.getAsString("date.is");
    this.includeCurrentDate = arguments.getAsBoolean("include.current.date", false);
        
    validateFieldId();
    validateDateIs();
    
    Object dateObject = workflowObject.getValue(fieldId);
    if (dateObject == null) {
      return null;
    }
    
    Calendar date = getDateFromObject(dateObject);
    
    Calendar now = Calendar.getInstance();
    if (dateObject instanceof DateOnly) {
      ensureDateOnly(now);
    }
    
    if (includeCurrentDate && now.equals(date)) {
      return null;
    }
    
    if (!doesMatchDateCondition(date, now)) {
      return "The selected date of the field \"" + workflowObject.getFieldLabel(fieldId) + "\" must lie in the " + dateIs + ".";
    }
    
    return null;
  }

  private void validateFieldId() {
    if (workflowObject.getFieldType(fieldId) == null) {
      throw new UserFriendlyRuntimeException("The field with the ID \"" + fieldId + "\" is not defined. Please check the configuration of the workflow condition " + CONDITION_NAME + ".");
    }
  }
  
  private void validateDateIs() {
    if (!Objects.equals(dateIs, "future") && !Objects.equals(dateIs, "past")) {
      throw new UserFriendlyRuntimeException("The date.is parameter of the workflow condition " + CONDITION_NAME + " needs to be future or past.");
    }
  }
  
  private Calendar getDateFromObject(Object dateObject) {
    Calendar calendar = Calendar.getInstance();

    if (dateObject instanceof DateOnly dateOnly) {
      calendar.setTime(dateOnly.getDate());
      ensureDateOnly(calendar);
    } else if (dateObject instanceof Date date) {
      calendar.setTime(date);
    } else {
        throw new UserFriendlyRuntimeException("The field \"" + workflowObject.getFieldLabel(fieldId) + "\" is not a Date or Date time field. Please check the configuration of the workflow condition " + CONDITION_NAME + ".");
    }
    

    return calendar;
}

  private void ensureDateOnly(Calendar calendar) {
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
  }
  
  private boolean doesMatchDateCondition(Calendar date, Calendar now) {
    return (Objects.equals(dateIs, "future") && date.compareTo(now) > 0) || (Objects.equals(dateIs, "past") && date.compareTo(now) > 0);
}

}

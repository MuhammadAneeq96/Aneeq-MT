package com.tulrfsd.polarion.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.spi.CustomTypedList;

public interface RegExUtils {
  
  public static boolean checkFieldSyntax(IWorkflowObject workflowObject, String fieldKey, String regEx, boolean requireAllEnumOptions) {
    
    Object fieldValue = workflowObject.getValue(fieldKey);
    
    if (fieldValue == null) {
      return true;
    } else if (fieldValue instanceof Text text) {
      return checkSyntax(text, regEx);
    } else if (fieldValue instanceof DurationTime durationTime) {
      return checkSyntax(durationTime, regEx);
    } else if (fieldValue instanceof DateOnly dateOnly) {
      return checkSyntax(dateOnly, regEx);
    } else if (fieldValue instanceof Date date) {
      return checkSyntax(date, regEx);
    } else if (fieldValue instanceof Integer number) {
      return checkSyntax(number.intValue(), regEx);
    } else if (fieldValue instanceof Float number) {
      return checkSyntax(number.floatValue(), regEx);
    } else if (fieldValue instanceof Currency currency) {
      return checkSyntax(currency, regEx);
    } else if (fieldValue instanceof TimeOnly timeOnly) {
      return checkSyntax(timeOnly, regEx);
    } else if (fieldValue instanceof String string) {
      return checkSyntax(string, regEx);
    } else if (fieldValue instanceof IEnumOption enumOption) {
      return checkSyntax(enumOption, regEx);
    } else if (fieldValue instanceof CustomTypedList list) {
      return checkSyntax(list, regEx, requireAllEnumOptions);
    } else {
      return true;
    }
    
  }
  

  public static boolean checkSyntax(String string, String regEx) {
    return string.matches(regEx);
  }
  
  public static boolean checkSyntax(int number, String regEx) {
    return checkSyntax(Integer.toString(number), regEx);
  }
  
  public static boolean checkSyntax(float number, String regEx) {
    return checkSyntax(Float.toString(number), regEx);
  }
  
  public static boolean checkSyntax(Currency amount, String regEx) {
    return checkSyntax(amount.toString(), regEx);
  }
  
  public static boolean checkSyntax(TimeOnly time, String regEx) {
    return checkSyntax(time.toString().split(" ")[0], regEx);
  }
  
  public static boolean checkSyntax(DateOnly dateOnly, String regEx) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    return checkSyntax(dateFormat.format(dateOnly.getDate()), regEx);
  }
  
  public static boolean checkSyntax(DurationTime duration, String regEx) {
    return checkSyntax(duration.toString(), regEx);
  }
  
  public static boolean checkSyntax(Date date, String regEx) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    return checkSyntax(dateFormat.format(date), regEx);
  }
  
  public static boolean checkSyntax(Text text, String regEx) {
    return checkSyntax(text.convertToPlainText().getContent(), regEx);
  }
  
  private static boolean checkSyntax(IEnumOption enumOption, String regEx) {
    return checkSyntax(enumOption.getId(), regEx);
  }
  
  private static boolean checkSyntax(CustomTypedList list, String regEx, boolean requireAllPass) {
    boolean result = list.isEmpty();
    for (Object object : list) {
      if (object instanceof IEnumOption enumOption) {
        result = result || checkSyntax(enumOption, regEx);
        if (requireAllPass && !result) {
          return false;
        } else if (!requireAllPass && result) {
          return true;
        }
      } else {
        throw new UserFriendlyRuntimeException("CustomTypedList does not contain EnumOptions in RegExUtils of the TULRFSD Workflow functions and conditions extensions.");
      }
    }
    return result;
  }
  
}
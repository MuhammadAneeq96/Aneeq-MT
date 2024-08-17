package com.tulrfsd.polarion.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.core.util.types.duration.DurationTime;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.spi.CustomTypedList;

public interface FieldRenderer {
  
  @NotNull
  public static String render(@Nullable Object fieldValue) {
    if (fieldValue == null) {
      return "";
    } else if (fieldValue instanceof Text text) {
      return render(text);
    } else if (fieldValue instanceof DurationTime durationTime) {
      return render(durationTime);
    } else if (fieldValue instanceof DateOnly dateOnly) {
      return render(dateOnly);
    } else if (fieldValue instanceof Date date) {
      return render(date);
    } else if (fieldValue instanceof Integer number) {
      return render(number);
    } else if (fieldValue instanceof Float number) {
      return render(number);
    } else if (fieldValue instanceof Currency currency) {
      return render(currency);
    } else if (fieldValue instanceof TimeOnly timeOnly) {
      return render(timeOnly);
    } else if (fieldValue instanceof String string) {
      return render(string);
    } else if (fieldValue instanceof IEnumOption enumOption) {
      return render(enumOption);
    } else if (fieldValue instanceof CustomTypedList list) {
      return render(list);
    } else {
      throw new UserFriendlyRuntimeException(String.format("The field type %s is not supported for rendering field values. Please check the workflow configuration.", fieldValue.getClass().getName()));
    }
  }
  
  @NotNull
  private static String render(@Nullable String string) {
    return string == null ? "" : string;
  }
  
  @NotNull
  private static String render(@Nullable Text text) {
    return text == null ? "" : text.getContent();
  }
  
  @NotNull
  private static String render(@Nullable IEnumOption enumOption) {
    return enumOption == null ? "" : enumOption.getName();
  }
  
  @SuppressWarnings("unchecked")
  @NotNull
  private static String render(@Nullable CustomTypedList list) {
    
    return list == null ? "" : (String) list.stream()
                                            .map(obj -> ((IEnumOption) obj).getName())
                                            .collect(Collectors.joining(", ", "[", "]"));
  }
  
  @NotNull
  private static String render(@Nullable Integer number) {
    return number == null ? "" : number.toString();
  }
  
  @NotNull
  private static String render(@Nullable Float number) {
    if (number == null) {
      return "";
    } else if (number.toString().contains(".")) {
      return String.format("%.2f", number);
    } else {
      return number.toString();
    }
  }
  
  @NotNull
  private static String render(@Nullable Currency number) {
    if (number == null) {
      return "";
    } else if (number.toString().contains(".")) {
      return String.format("%.2f", number);
    } else {
      return number.toString();
    }
  }
  
  @NotNull
  private static String render(@Nullable TimeOnly time) {
    return time == null ? "" : time.toString().split(" ")[0];
  }
  
  @NotNull
  private static String render(@Nullable DateOnly dateOnly) {
    return dateOnly == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(dateOnly.getDate());
  }
  
  @NotNull
  private static String render(@Nullable Date date) {
    return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
  }
  
  @NotNull
  private static String render(@Nullable DurationTime duration) {
    return duration == null ? "" : duration.toString();
  }

}

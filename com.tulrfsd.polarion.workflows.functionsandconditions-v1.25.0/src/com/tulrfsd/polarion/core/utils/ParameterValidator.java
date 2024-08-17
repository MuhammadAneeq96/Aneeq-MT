package com.tulrfsd.polarion.core.utils;

import org.jetbrains.annotations.Nullable;

public interface ParameterValidator {
  
  public static void requireNonNull(@Nullable Object object, @Nullable String variableName) {
    
    if (object == null) {
      throw new IllegalArgumentException(String.format("The parameter %s must not be null.", getVariableName(variableName)));
    } 
  }
  
  public static void requireNonEmptyString(@Nullable String string, @Nullable String variableName) {
    requireNonNull(string, variableName);
    if (string.isEmpty()) {
      throw new IllegalArgumentException(String.format("The parameter %s must not be empty.", getVariableName(variableName)));
    }
  }
  
  private static String getVariableName(@Nullable String variableName) {
    return variableName == null || variableName.isEmpty() ? "(name unknown)" : variableName;
  }
}
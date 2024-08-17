package com.tulrfsd.polarion.workflows.utils;

public enum Literals {
  
  WORKFLOW_PREFIX("tulrfsd_");
  
  private final String value;
  
  Literals(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }


}

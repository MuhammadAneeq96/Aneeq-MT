package com.tulrfsd.polarion.core.utils;

public enum CoreLiterals {
  
  WORKITEM_RTE_LINK_REGEX("(<span class=\\\"polarion-rte-link\\\" data-type=\\\"workItem\\\" id=\\\"fake\\\"(?: data-scope=\\\"([\\w_.]+)\\\")? data-item-id=\\\"([\\w-]+)\\\" data-option-id=\\\"(long|short)\\\"(?: data-revision=\\\"(\\d+)\\\")?></span>)"),
  WORKITEM_RENDER_LINK_REGEX("(#/project/([\\w_.]+)/workitem\\?id=([\\w-]+)(?:&amp;revision=(\\d+))?)"),
  MODULE_RTE_LINK_REGEX("(<span class=\\\"polarion-rte-link\\\" data-type=\\\"document\\\" id=\\\"fake\\\"(?: data-scope=\\\"([\\w_.]+)\\\")? data-item-name=\\\"([\\w-_ ]+)\\\" data-space-name=\\\"([\\w-_ ]+)\\\" data-option-id=\\\"default\\\"(?: data-revision=\\\"(\\d+)\\\")?></span>)"),
  TESTRUN_RTE_LINK_REGEX("(<span class=\\\"polarion-rte-link\\\" data-type=\\\"testRun\\\" id=\\\"fake\\\"(?: data-scope=\\\"([\\w_.]+)\\\")? data-item-id=\\\"([\\w-_ ]+)\\\" data-option-id=\\\"(long|short)\\\"(?: data-revision=\\\"(\\d+)\\\")?></span>)"),
  LOGGER("com.tulrfsd.polarion.core");
  
  private final String value;
  
  CoreLiterals(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }


}

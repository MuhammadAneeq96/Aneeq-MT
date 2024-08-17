package com.tulrfsd.polarion.core.utils;

import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.security.ISecurityService;

public interface ServicesProvider {
  
  static ITrackerService trackerService = PlatformContext.getPlatform().lookupService(ITrackerService.class);
  static IDataService dataService = trackerService.getDataService();
  static IProjectService projectService = trackerService.getProjectsService();
  static ITestManagementService testManagementService = ((ITestRun) dataService.createInstance(ITestRun.PROTO)).getTestManagementService();
  static ISecurityService securityService = dataService.getSecurityService();
  
  public static ITrackerService getTrackerService() {
    return trackerService;
  }
  
  public static IDataService getDataService() {
    return dataService;
  }
  
  public static IProjectService getProjectService() {
    return projectService;
  }
  
  public static ITestManagementService getTestManagementService() {
    return testManagementService;
  }
  
  public static ISecurityService getSecurityService() {
    return securityService;
  }

}

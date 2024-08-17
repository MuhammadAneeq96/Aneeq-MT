package com.tulrfsd.polarion.core.utils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.shared.api.model.user.User;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.platform.security.ISecurityService;

public interface ProjectCoreHelper {
  
  @NotNull
  public static Collection<String> getUserIdsForRole(@NotNull String projectId, @Nullable String roleId) {
    ParameterValidator.requireNonEmptyString(projectId, "projectId");
    
    ISecurityService securityService = ServicesProvider.getSecurityService(); 
    IProject project = ServicesProvider.getProjectService().getProject(projectId);
    
    if (roleId != null && project != null && securityService.getContextRoles(project.getContextId()).contains(roleId)) {
      return securityService.getUsersForContextRole(roleId, project.getContextId());
    } else {
      return securityService.getUsersForGlobalRole((roleId == null) ? "user" : roleId);
    }
  }
  
  @NotNull
  public static Map<String, IUser> getIUsersForRole(@NotNull String projectId, @Nullable String roleId) {
    ParameterValidator.requireNonEmptyString(projectId, "projectId");
    
    IProjectService projectService = ServicesProvider.getProjectService();    
    return getUserIdsForRole(projectId, roleId).stream()
                                               .collect(Collectors.toMap(
                                                   userId -> userId, 
                                                   projectService::getUser));
  }
  
  @NotNull
  public static Map<String, User> getUsersForRole(@NotNull String projectId, @Nullable String roleId) {
    ParameterValidator.requireNonEmptyString(projectId, "projectId");
    
    ReadOnlyTransaction transaction = Transaction.getTransaction();       
    return getUserIdsForRole(projectId, roleId).stream()
                                               .collect(Collectors.toMap(
                                                   userId -> userId, 
                                                   userId -> transaction.users().getBy().id(userId)));
  }
  
  @NotNull
  public static String getProjectNameOrId(@NotNull IProject project) {
    if (project.getName() != null) {
      return project.getName();
    } else {
      return project.getId();
    }    
  }

}

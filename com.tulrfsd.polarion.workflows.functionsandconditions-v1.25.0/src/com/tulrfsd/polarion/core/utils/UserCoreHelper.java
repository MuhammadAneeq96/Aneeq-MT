package com.tulrfsd.polarion.core.utils;

import org.jetbrains.annotations.NotNull;
import com.polarion.alm.projects.model.IUser;

public interface UserCoreHelper {
  
  @NotNull
  public static IUser getCurrentUser() {
    return ServicesProvider.getProjectService().getUser(ServicesProvider.getSecurityService().getCurrentUser());
  }

}

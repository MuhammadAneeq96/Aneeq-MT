package com.tulrfsd.polarion.core.utils;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import java.util.ArrayList;
import java.util.HashSet;

public interface HyperlinkCoreHelper {
  
  @Nullable
  public static String getRTEHyperlink(@NotNull IWorkItem workItem, @Nullable String projectId, boolean withTitle) {
    ParameterValidator.requireNonNull(workItem, "workItem");
    return getRTEHyperlink(workItem, workItem.getRevision(), projectId, withTitle, "workItem");
  }
  
  @Nullable
  public static String getRTEHyperlink(@NotNull IWorkItem workItem, @Nullable String revision, @Nullable String projectId, boolean withTitle) {
    ParameterValidator.requireNonNull(workItem, "workItem");
    return getRTEHyperlink(workItem, revision, projectId, withTitle, "workItem");
  }
  
  @Nullable
  public static String getRTEHyperlink(@NotNull ITestRun testRun, @Nullable String projectId, boolean withTitle) {
    ParameterValidator.requireNonNull(testRun, "testRun");
    return getRTEHyperlink(testRun, testRun.getRevision(), projectId, withTitle, "testRun");
  }
  
  @Nullable
  public static String getRTEHyperlink(@NotNull ITestRun testRun, @Nullable String revision, @Nullable String projectId, boolean withTitle) {
    ParameterValidator.requireNonNull(testRun, "testRun");
    return getRTEHyperlink(testRun, revision, projectId, withTitle, "testRun");
  }
  
  @Nullable
  public static String getRTEHyperlink(@NotNull IModule module, @Nullable String projectId) {
    ParameterValidator.requireNonNull(module, "module");
    return getRTEHyperlink(module, module.getRevision(), projectId);
  }
  
  @Nullable
  public static String getRTEHyperlink(@NotNull IModule module, @Nullable String revision, @Nullable String projectId) {
    ParameterValidator.requireNonNull(module, "module");
    
    if (module.isUnresolvable() || module.getModuleNameWithSpace() == null) {
      return null;
    }
    StringBuilder link = new StringBuilder("<span class=\"polarion-rte-link\" data-type=\"document\" id=\"fake\"");
    if (!module.getProjectId().equals(projectId)) {
      link.append(String.format(" data-scope=\"%s\"", module.getProjectId()));
    }
    link.append(String.format(" data-item-name=\"%s\"", module.getId()));
    link.append(String.format(" data-space-name=\"%s\" data-option-id=\"default\"", module.getModuleFolder()));
    if (revision != null) {
      link.append(String.format(" data-revision=\"%s\"", revision));
    }
    link.append("></span>");
    
    return link.toString();
  }
  
  @Nullable
  private static String getRTEHyperlink(@NotNull IWorkflowObject object, @Nullable String revision, @Nullable String projectId, boolean withTitle, @NotNull String prototypeId) {
    if (object.isUnresolvable() || object.getId() == null || !(object instanceof IWorkItem || object instanceof ITestRun )) {
      return null;
    }
    StringBuilder link = new StringBuilder();
    link.append(String.format("<span class=\"polarion-rte-link\" data-type=\"%s\" id=\"fake\"", prototypeId));
    if (!object.getProjectId().equals(projectId)) {
      link.append(String.format(" data-scope=\"%s\"", object.getProjectId()));
    }
    link.append(String.format(" data-item-id=\"%s\" data-option-id=\"%s\"", object.getId(), withTitle ? "long" : "short"));
    if (revision != null) {
      link.append(String.format(" data-revision=\"%s\"", revision));
    }
    link.append("></span>");
    
    return link.toString();
  }
  
  @NotNull
  public static List<IWorkItem> getWorkItemsFromHyperlinksInHtml(@NotNull String content, @NotNull String currentProjectId) {
    ParameterValidator.requireNonNull(content, "content");
    ParameterValidator.requireNonEmptyString(currentProjectId, "currentProjectId");
    
    List<IWorkItem> list = new ArrayList<>();
    
    Set<String> regularExpressions = new HashSet<>();
    // links added via the Rendering API contain the URL to the workitem
    regularExpressions.add(CoreLiterals.WORKITEM_RENDER_LINK_REGEX.getValue());
    // links added via the UI are polarion-rte-links storing the work item information in html element attributes
    regularExpressions.add(CoreLiterals.WORKITEM_RTE_LINK_REGEX.getValue());
    
    for (String regularExpression : regularExpressions) {
      Pattern linkPattern = Pattern.compile(regularExpression);
      // search text of specified fields for work item links
      Matcher matcher = linkPattern.matcher(content);
      while (matcher.find()) {
        String projectId = matcher.group(2);
        if (projectId == null || projectId.isEmpty()) {
          projectId = currentProjectId;
        }
        String workItemId = matcher.group(3);
        String revision = "";
        if (regularExpression.equals(CoreLiterals.WORKITEM_RTE_LINK_REGEX.getValue())) {
          revision = matcher.group(5);
        } else if (regularExpression.equals(CoreLiterals.WORKITEM_RENDER_LINK_REGEX.getValue())) {
          revision = matcher.group(4);
        }
        
        IWorkItem workItem = ServicesProvider.getTrackerService().getWorkItemWithRevision(projectId, workItemId, revision);
        if (workItem != null && !workItem.isUnresolvable() && !list.contains(workItem)) {
          list.add(workItem);
        }
      }
    }
    
    return list;
  }

}

package com.tulrfsd.polarion.workflows.generic.functions;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRow;
import com.polarion.alm.tracker.model.ITable;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.types.Text;
import com.polarion.subterra.base.location.ILocation;
import com.tulrfsd.polarion.core.utils.CoreLiterals;
import com.tulrfsd.polarion.core.utils.HyperlinkCoreHelper;
import com.tulrfsd.polarion.core.utils.ServicesProvider;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class SetRevisionOfWorkflowObjectHyperlinks implements IFunction<IWorkflowObject> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + SetRevisionOfWorkflowObjectHyperlinks.class.getSimpleName();
  IWorkflowObject workflowObject;
  boolean useLastStatusTransition;
  boolean overwriteRevision;
  boolean removeRevision;
  String currentProjectId;

  @Override
  public void execute(ICallContext<IWorkflowObject> context, IArguments arguments) {
    
    this.workflowObject = context.getTarget();
    this.currentProjectId = workflowObject.getProjectId();
    Set<String> fieldIds = arguments.getAsSet("field.ids");
    this.useLastStatusTransition = arguments.getAsBoolean("use.last.status.transition", false);
    this.overwriteRevision = arguments.getAsBoolean("overwrite.revision", false);
    this.removeRevision = arguments.getAsBoolean("remove.revision", false);
    
    for (String fieldId : fieldIds) {
      if (!WorkflowObjectCoreHelper.isTextField(workflowObject, fieldId) && !WorkflowObjectCoreHelper.isTableField(workflowObject, fieldId)) {
        throw new IllegalArgumentException(String.format("The workflow function %s only supports RichText and Table fields. The fieldId %s does not belong to a field of any of those types.", FUNCTION_NAME, fieldId));
      }
      
      Object field = workflowObject.getValue(fieldId);
      if (field instanceof Text text && text.isPlain()) {
        throw new IllegalArgumentException(String.format("The workflow function %s only supports RichText and Table fields. The field %s is not a RichText field, but only a Plain Text field.", FUNCTION_NAME, workflowObject.getFieldLabel(fieldId)));
      }
      
      if (field instanceof Text text && text.isHtml()) {
        text = processRichText(text);
        workflowObject.setValue(fieldId, text);
      }
        else if (field instanceof ITable table) {
        processTable(table);
      }
    }
  }
  
  @NotNull 
  private Text processRichText(@NotNull Text text) {
    text = updateWorkItemLinks(text);
    text = updateModuleLinks(text);
    return updateTestRunLinks(text);
  }
  
  @NotNull
  private Text updateWorkItemLinks(@NotNull Text text) {
    Pattern linkPattern = Pattern.compile(CoreLiterals.WORKITEM_RTE_LINK_REGEX.getValue());
    String content = text.getContent();
    Matcher matcher = linkPattern.matcher(text.getContent());
    while (matcher.find()) {
      String oldLink = matcher.group(1);
      String projectId = matcher.group(2);
      if (projectId == null || projectId.isEmpty()) {
        projectId = currentProjectId;
      }
      String workItemId = matcher.group(3);
      boolean withTitle = matcher.group(4).equals("long");
      String revision = matcher.group(5);
      
      IWorkItem workItem = getWorkItem(projectId, workItemId);
      
      String newLink = getNewLink(workItem, revision, workflowObject.getProjectId(), withTitle);
      if (newLink != null) {
        content = content.replace(oldLink, newLink);
      }
    }
    return Text.html(content);
  }

  @NotNull 
  private Text updateModuleLinks(@NotNull Text text) {
    Pattern linkPattern = Pattern.compile(CoreLiterals.MODULE_RTE_LINK_REGEX.getValue());
    String content = text.getContent();
    Matcher matcher = linkPattern.matcher(text.getContent());
    while (matcher.find()) {
      String oldLink = matcher.group(1);
      String projectId = matcher.group(2);
      if (projectId == null || projectId.isEmpty()) {
        projectId = currentProjectId;
      }
      String moduleId = matcher.group(3);
      String spaceId = matcher.group(4);
      String revision = matcher.group(5);
      
      IModule module = getModule(projectId, moduleId, spaceId);
      
      String newLink = getNewLink(module, revision, workflowObject.getProjectId(), false);
      if (newLink != null) {
        content = content.replace(oldLink, newLink);
      }
    }
    return Text.html(content);
  }

  @NotNull 
  private Text updateTestRunLinks(@NotNull Text text) {
    Pattern linkPattern = Pattern.compile(CoreLiterals.TESTRUN_RTE_LINK_REGEX.getValue());
    String content = text.getContent();
    Matcher matcher = linkPattern.matcher(text.getContent());
    while (matcher.find()) {
      String oldLink = matcher.group(1);
      String projectId = matcher.group(2);
      if (projectId == null || projectId.isEmpty()) {
        projectId = currentProjectId;
      }
      String testRunId = matcher.group(3);
      boolean withTitle = matcher.group(4).equals("long");
      String revision = matcher.group(5);
      
      ITestRun testRun = getTestRun(projectId, testRunId);
      
      String newLink = getNewLink(testRun, revision, workflowObject.getProjectId(), withTitle);
      if (newLink != null) {
        content = content.replace(oldLink, newLink);
      }
    }
    return Text.html(content);
  }


  
  private void processTable(@NotNull ITable table) {
    table.getRows().forEach(this::processRow);
  }

  @SuppressWarnings("unchecked")
  private void processRow(@NotNull IRow row) {
    List<Text> cells = (List<Text>) row.getValues();
    for (int i = 0; i < cells.size(); i++) {
      cells.set(i, processRichText(cells.get(i)));
    }
  }
  
  @NotNull
  private IWorkItem getWorkItem(@NotNull String projectId, @NotNull String workItemId) {
    IWorkItem workItem = ServicesProvider.getTrackerService().findWorkItem(projectId, workItemId);
    if (workItem == null || workItem.isUnresolvable()) {
      throw new IllegalArgumentException(String.format("The work item with the ID \"%s\" is not resolvable or does not exist anymore. Please fix the content of %s %s before repeating the status transition.",
          workItemId, workflowObject.getPrototype().getName(), workflowObject.getId()));
    }
    return workItem;
  }
  
  @NotNull
  private IModule getModule(@NotNull String projectId, @NotNull String moduleId, @NotNull String spaceId) {
    ITrackerProject project = ServicesProvider.getTrackerService().getTrackerProject(projectId);
    if (project.isUnresolvable()) {
      throw new IllegalArgumentException(String.format("The project with the ID \"%s\" does not exist or is not resolvable.", projectId));
    }
    ILocation projectLocation = project.getLocation();
    ILocation relativeModuleLocation = projectLocation.append(spaceId)
                                                      .append(moduleId)
                                                      .getRelativeLocation(projectLocation);    
    IModule module = ServicesProvider.getTrackerService().getModuleManager().getModule(project, relativeModuleLocation);
    if (module == null || module.isUnresolvable()) {
      throw new IllegalArgumentException(String.format("The document with the ID \"%s\" is not resolvable or does not exist anymore. Please fix the content of %s %s before repeating the status transition.",
          moduleId, workflowObject.getPrototype().getName(), workflowObject.getId()));
    }
    return module;
  }
  
  @NotNull
  private ITestRun getTestRun(@NotNull String projectId, @NotNull String testRunId) {
    ITestRun testRun = ServicesProvider.getTestManagementService().getTestRun(projectId, testRunId);
    if (testRun == null || testRun.isUnresolvable()) {
      throw new IllegalArgumentException(String.format("The test run with the ID \"%s\" is not resolvable or does not exist anymore. Please fix the content of %s %s before repeating the status transition.",
          testRunId, workflowObject.getPrototype().getName(), workflowObject.getId()));
    }
    return testRun;
  }
  
  @Nullable
  private String getNewLink(@NotNull IWorkflowObject object, @Nullable String revision, @NotNull String projectId, boolean withTitle) {
    
    if (!overwriteRevision && !removeRevision && revision != null && !revision.isEmpty()) {
      return null;
    } else if (removeRevision) {
      revision = null;
    } else if (useLastStatusTransition) {
      revision = WorkflowObjectCoreHelper.getRevisionOfLastStatusChange(object, null);
    } else {
      revision = object.getLastRevision();
    }
    
    String newLink = "";
    if (object instanceof IWorkItem workItem) {
      newLink = HyperlinkCoreHelper.getRTEHyperlink(workItem, revision, projectId, withTitle);
    } else if (object instanceof IModule module) {
      newLink = HyperlinkCoreHelper.getRTEHyperlink(module, revision, projectId);
    } else if (object instanceof ITestRun testRun) {
      newLink = HyperlinkCoreHelper.getRTEHyperlink(testRun, revision, projectId, withTitle);
    }
    return newLink;
  }
  
}

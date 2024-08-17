## Introduction

This extension provides several workflow functions and conditions for Work Items, Documents (Modules), and Test Runs. See the definition below for a full description of the implemented functions and conditions and their configuration parameters.

The names of the functions and conditions have the prefix `tulrfsd_` to easily identify them in the configuration panel and to avoid naming conflicts.

## License

Download and use of the extension is free. By downloading the extension, you accept the license terms and confirm to have a valid Polarion license issued by Siemens.

See [COPYRIGHT](COPYRIGHT), [LICENSE](LICENSE), and [DCO](DCO) for licensing details.

## Configuration
Refer to the Polarion Help for general questions regarding the Polarion Setup.

## Dependencies

This extension requires the [TULRFSD Polarion Core Extension](../../../../com.tulrfsd.polarion.core). Check the release notes for the minimum required version of this extension.

## Extension Deployment
 - Build the plugin yourself or use one of the precompiled [releases](../../releases)
 - Stop the Polarion server
 - Go to `<Polarion INSTALL>/polarion/extensions`.
 - If the subfolder `tulrfsd` does not yet exist, create it.
 - In this subfolder, create the folder structure `eclipse/plugins`.
 - Copy the jar file `com.tulrfsd.polarion.workflows.functionsandconditions<version_number>` in here.
 - Remove any existing older versions of the same plugin.
 - Delete the folder `<Polarion INSTALL>/data/workspace/.config`
 - Start the Polarion Server

## Requirements
- Polarion 2310

# General

## Implementation Details

This extension contains the `ICustomWorkflowCondition<T extends IWorkflowObject>` interface which extends the `IWorkflowCondition<T extends IWorkflowObject>` of Polarion. The benefits of the custom interface are:
- The deprecated method `boolean passesCondition(ICallContext<T> context, IArguments arguments)` does not need to be implemented for each workflow condition.
- When using the custom method `public String checkCondition(ICallContext<T> context, IArguments arguments)`, Polarion correctly renders the error message of any exception thrown by the workflow condition in the UI instead of the meaningless message *condition XYZ did not pass check*.

# All Types

The following conditions and functions are available for WorkItems, Modules (Documents), and TestRuns.

## Conditions

### AllUsersSelectedInFieldSigned

This condition passes if all users selected in the fields specified by `field.ids` have signed the workflow action to the new status defined by `target.status.id`.

| Parameter        | Description                                                   | Example            | Mandatory          | Comment                                                             |
|------------------|---------------------------------------------------------------|--------------------|--------------------|---------------------------------------------------------------------|
| field.ids        | The IDs of the fields where the users are specified.          | reviewer, approver | <center>X</center> | Comma-separated values, Field Type: Single/Multi Enum               |
| target.status.id | The ID of the target status for which the users need to sign. | approved           |                    | If not specified, the user may have signed for any workflow action. |

### CheckDateField

This condition passes if the date in the field `field.id` is in the future or in the past, depending on the `date.is` parameter.

| Parameter            | Description                                    | Example | Mandatory          | Comment                      |
|----------------------|------------------------------------------------|---------|--------------------|------------------------------|
| field.id             | The ID of the date field.                      | dueDate | <center>X</center> | Field Type: Date or DateOnly |
| date.is              | Date needs to be in the future or in the past. | future  | <center>X</center> | Valid values: future, past   |
| include.current.date | Date may also be today.                        | true    |                    | Default value: false         |

### CheckFieldRegEx

This condition passes if the field values match the regular expression. This condition supports checking multiple fields at once. Empty fields or fields with a type that is not supported pass the condition.
The regular expression is configured with the parameter `regex.<fieldId>`.
If the check fails, the condition can also display an optional custom error message configured with `message.<fieldId>`.
A standard error message will be shown if no error message is configured.

| Parameter                     | Description                                                                         | Example                                                          | Mandatory          | Comment                                                                                                                             |
|-------------------------------|-------------------------------------------------------------------------------------|------------------------------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| regex.\<fieldId>              | The regular expression for the field.                                               | regex.email = ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$   | <center>X</center> | Field Type: String, Text, Date, DateOnly, TimeOnly, DurationTime, Integer, Float, Currency, Single/Multi Enum (Checks ID, not Name) |
| message.\<fieldId>            | The custom error message for the field.                                             | message.email = You need to provide a valid email address.       |                    | If not specified, the condition will show the basic error message.                                                                  |
| require.all.enum.options.pass | For multi-select Enum fields, require all selected options to match the expression. | true                                                             |                    | Default value: false                                                                                                                |

### CheckForExistingWorkflowSignature

This condition passes if the user has not yet signed the workflow action and the parameter `existing.signature` is `false`.
If `existing.signature` is `true`, users can only sign when they have already signed before.
This condition is only useful when the workflow action does not change the workflow status so that multiple users can sign for the same target status.

| Parameter          | Description                                                          | Example | Mandatory | Comment              |
|--------------------|----------------------------------------------------------------------|---------|-----------|----------------------|
| existing.signature | The user shall already have signed for this workflow action, or not. | false   |           | Default value: false |

### CheckForObjectsWithEqualFieldValues

This condition evaluates if there are other work items, documents, or test runs matching the `query` with equal fields defined in `field.ids.equal` or unequal fields defined in `field.ids.unequal`.
This condition can optionally be executed as system user to account for objects the current user does not have access to.

| Parameter         | Description                                                                                                         | Example                 | Mandatory          | Comment                |
|-------------------|---------------------------------------------------------------------------------------------------------------------|-------------------------|--------------------|------------------------|
| query             | The query to search for the other objects                                                                           | type:system_requirement | <center>X</center> |                        |
| check.global      | By default, the search is only executed in the same project. The search can be extended to the complete repository. | true                    |                    | Default value: false   |
| exclude.workitems | Exclude work items from the search.                                                                                 | false                   |                    | Default value: false   |
| exclude.documents | Exclude modules (documents) from the search.                                                                        | false                   |                    | Default value: false   |
| exclude.testruns  | Exclude test runs from the search.                                                                                  | false                   |                    | Default value: false   |
| field.ids.equal   | The IDs of the fields that shall be equal.                                                                          | field1, field2          |                    | Comma-separated values |
| field.ids.unequal | The IDs of the fields that shall be unequal                                                                         | field1, field2          |                    | Comma-separated values |
| use.system.user   | Use System User to override permission settings. The action is still executed as the same user.          | true                   |           | Default value: false   |

### CurrentUserSelectedInField

This condition passes if the current user is selected in one of the fields specified by `field.ids`.

| Parameter | Description                                                           | Example             | Mandatory          | Comment                                                 |
|-----------|-----------------------------------------------------------------------|---------------------|--------------------|---------------------------------------------------------|
| field.ids | The IDs of the fields where the users that need to sign are selected. | reviewers, approver | <center>X</center> | Comma-separated values<br>Field Type: Single/Multi Enum |

### ReferencedObjectMatchesQuery

This condition passes if all the objects selected in the enum fields defined by `field.ids` match the `query`. This condition also supports the Document field of test runs.

| Parameter     | Description                                              | Example                                     | Mandatory          | Comment                                                                 |
|---------------|----------------------------------------------------------|---------------------------------------------|--------------------|-------------------------------------------------------------------------|
| field.ids     | The IDs of the fields to check.                          | requirements, test_cases                    | <center>X</center> | Field Type: Single/Multi Enum                                           |
| query         | The query the referenced objects need to match.          | `type:(requirement) AND status:(approved)`  | <center>X</center> |                                                                         |
| scope.project | Limit the search to project scope or search globally.    | true                                        |                    | default value: true                                                     |
| message       | A custom error message to show when the condition fails. | The referenced requirement is not approved. |                    | A default message will be shown if the custom message is not specified. |

### ReferencingObjectMatchesQuery

This condition passes if there is a work item, module (document), or test run that matches the query and references the current object in an enum field.
The query needs to contain placeholders to insert the IDs of the current object.  
Example: `type:(tool) AND artifact.KEY:({{projectId}}/{{objectId}})` will be processed to the query `type:(tool) AND artifact.KEY:(TestProject/T\-123)`. For documents, there is also the placeholder `{{spaceId}}`.

It depends on the enumeration factories installed on the Polarion server how the objects are stored in the enumeration.
To identify the correct query for the server, use the tracker search bar or a table block widget to generate the query and then convert it to text.

The condition can be activated or deactivated based on field values. Similar to the CheckFieldRegEx workflow condition, multiple `regex.<fieldId>` can be defined that must pass to activate this workflow condition.

This condition can `use.system.user` to return correct results even if the user executing the workflow action does have access to all resources.

| Parameter              | Description                                                                       | Example                                                     | Mandatory          | Comment                                                                                    |
|------------------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------|--------------------|--------------------------------------------------------------------------------------------|
| prototype              | The object type that shall reference the current object.                          | WorkItem                                                    | <center>X</center> | Valid values: WorkItem, Module, TestRun                                                    |
| query                  | The query to search for the objects referencing the current object.               | `type:(tool) AND artifact.KEY:({{projectId}}/{{objectId}})` | <center>X</center> | Available placeholders: `{{projectId}}`, `{{objectId}}` (`{{spaceId}}` for documents only) |
| scope.project          | Limit the search to project scope or search globally.                             | true                                                        |                    | default value: true                                                                        |
| min                    | The minimum allowed number of objects referencing the current object.             |                                                             |                    | Default value: 1                                                                           |
| max                    | The maximum allowed number of objects referencing the current object.             |                                                             |                    | Default value: maximum integer                                                             |
| message                | A custom error message to show when the condition fails.                          | No Task references this Test Run.                           |                    | A default message will be shown if the custom message is not specified.                    |
| regex.\<fieldId>       | One or many RegEx conditions that activate or deactivate this workflow condition. | regex.verification_method=testing                           |                    | See instructions for CheckFieldRegEx workflow condition.                                   |
| require.all.regex.pass | Require all RegEx conditions to pass (true) or just at least one (false).         | true                                                        |                    | Default value: false                                                                       |
| use.system.user        | Use System User to override permission settings.                                  | true                                                        |                    | Default value: false                                                                       |

### StatusAvailableForReferencingObject

This condition passes if the status with the `target.status.id` is available for all objects accessible to the user, matching the `query` and referencing this object in an enum field.
This condition can `use.system.user` to return correct results even if the user executing the workflow action does have access to all resources.

| Parameter        | Description                                                              | Example                                     | Mandatory          | Comment                                                                                    |
|------------------|--------------------------------------------------------------------------|---------------------------------------------|--------------------|--------------------------------------------------------------------------------------------|
| prototype        | The object type that shall reference the current object.                 | WorkItem                                    | <center>X</center> | Valid values: WorkItem, Module, TestRun                                                    |
| query            | The query to search for the objects referencing the current object.      | `objectId.KEY:({{projectId}}/{{objectId}})` | <center>X</center> | Available placeholders: `{{projectId}}`, `{{objectId}}` (`{{spaceId}}` for documents only) |
| target.status.id | The ID of the status that shall be available for the referencing object. | draft                                       | <center>X</center> |                                                                                            |
| scope.project    | Limit the search to project scope or search globally.                    | true                                        |                    | Default value: true                                                                        |
| use.system.user  | Use System User to override permission settings.                         | true                                        |                    | Default value: false                                                                       |

### UserSelectedInFieldHasRole

This condition passes if at least one user selected in the enum `field.ids` has the `role.id`. Alternatively, the condition can be configured to `require.all.have.role` or at least `require.one.in.each.field.has.role`. For the latter, the condition can be configured to `allow.empty.fields`.

| Parameter                          | Description                                                                  | Example                       | Mandatory          | Comment                |
|------------------------------------|------------------------------------------------------------------------------|-------------------------------|--------------------|------------------------|
| role.id                            | The ID of the role, the user(s) shall have.                                  | project_assignable            | <center>X</center> |                        |
| field.ids                          | The IDs of the fields to check.                                              | reviewer, release_responsible | <center>X</center> | Comma-separated values |
| require.all.have.role              | Every users selected in the fields needs to have the role.                   | true                          |                    | Default value: false   |
| require.one.in.each.field.has.role | At least one user in each field needs to have the role.                      | true                          |                    | Default value: false   |
| allow.empty.fields                 | Skips empty fields for the require.one.in.each.field.has.role configuration. | false                         |                    |                        |

## Functions

### CreateProjectBaseline

This function creates a project baseline with a configurable baseline name. The baseline is created for HEAD + 1 revision, so the one created by this action.
However, there is a very tiny chance of a race condition where another user performs a change between the time the HEAD revision is read from the database and the time the transaction is closed.
In that case, the baseline would point to the revision created by the other user. To reduce the risk of a race condition, this workflow function should not be followed up by extensive calculations caused by subsequent workflow conditions and functions.

| Parameter | Description                                                          | Example               | Mandatory | Comment                                                                                                                                                    |
|-----------|----------------------------------------------------------------------|-----------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| field.ids | The IDs of the fields to be added between the prefix and the suffix. | title, version        |           | Works best with String fields.<br>Supported Field Types: String, Text, Date, DateOnly, TimeOnly, DurationTime, Integer, Float, Currency, Single/Multi Enum |
| prefix    | The starting text of the project baseline name.                      | This baseline is for  |           | The function does not add an automatic space after the prefix.                                                                                             |
| suffix    | The closing text of the project baseline name                        | .                     |           | The function does not add an automatic space before the suffix.                                                                                            |

### ExecuteReferencedObjectWorkflowAction

This workflow function executes the workflow action of work items, modules (documents), or test runs referenced in the specified single/multi enum fields. Make sure that the query contains the status of the referenced object so that the function does not fail for objects that are already in the target status. The referenced objects are retrieved as system user but the workflow action is performed as the normal user executing the workflow action of this object. This approach ensures that no objects are missed because of access restrictions. The function fails if the workflow action cannot be performed for one of the referenced objects (due to insufficient permissions).

| Parameter        | Description                                                             | Example                               | Mandatory          | Comment                                                |
|------------------|-------------------------------------------------------------------------|---------------------------------------|--------------------|--------------------------------------------------------|
| field.ids        | The IDs of the fields containing the object references                  | items                                 | <center>X</center> | Comma-separated values, Field Types: Single/Multi Enum |
| query            | The query to filter the objects for which the action will be performed. | type:(requirement) AND status:(draft) | <center>X</center> |                                                        |
| action.id        | The ID of the workflow action                                           | start_review                          | <center>X</center> |                                                        |
| target.status.id | The ID of the target status for the workflow action.                    | in_review                             | <center>X</center> |                                                        |
| scope.project    | Only perform the action for objects in the same project.                | true                                  |                    | Default value: false                                   |

### ExecuteReferencingObjectWorkflowAction

This workflow function executes the workflow action of work items, modules (documents), or test runs referencing the current object in a single/multi enum field based on the specified query. Make sure that the query contains the status of the referencing objects so that the function does not fail for objects that are already in the target status. The referencing objects are retrieved as system user but the workflow action is performed as the normal user executing the workflow action of this object. This approach ensures that no objects are missed because of access restrictions. The function fails if the workflow action cannot be performed for one of the referencing objects (due to insufficient permissions).

| Parameter        | Description                                                         | Example                                                     | Mandatory          | Comment                                                                                    |
|------------------|---------------------------------------------------------------------|-------------------------------------------------------------|--------------------|--------------------------------------------------------------------------------------------|
| prototype        | The object type that shall reference the current object.            | WorkItem                                                    | <center>X</center> | Valid values: WorkItem, Module, TestRun                                                    |
| query            | The query to search for the objects referencing the current object. | `type:(tool) AND artifact.KEY:({{projectId}}/{{objectId}})` | <center>X</center> | Available placeholders: `{{projectId}}`, `{{objectId}}` (`{{spaceId}}` for documents only) |
| action.id        | The ID of the workflow action.                                      | start_review                                                | <center>X</center> |                                                                                            |
| target.status.id | The ID of the target status for the workflow action.                | in_review                                                   | <center>X</center> |                                                                                            |
| scope.project    | Only perform the action for objects in the same project.            | true                                                        |                    | Default value: false                                                                       |

### ResetFieldValue

This function resets the value of custom and built-in fields to the default value. If no default value is specified, the field is cleared.
This function supports the field types String, Text, RichText, Integer, Float, Currency, Time, Date, Date time, Duration, Boolean, Table, Enum, Test Steps.
For enum custom fields, the default value specified for the custom field has precedence over the enumeration default value.

| Parameter    | Description                                                    | Example                            | Mandatory          | Comment               |
|--------------|----------------------------------------------------------------|------------------------------------|--------------------|-----------------------|
| field.ids    | The IDs of the fields to be reset.                             | priority, responsible, description | <center>X</center> | Comma-separated values|
| always.clear | Always clears the field even if a default value is configured. | true                               |                    | Default value: false  |

### SetEnumOptions

This function sets the `option.ids` in the field with the `field.id`. Multiply enum fields can optionally be cleared before adding the enum option(s).
By default, the function throws an error if an enum option is not available for the field.

| Parameter              | Description                                                       | Example              | Mandatory          | Comment                |
|------------------------|-------------------------------------------------------------------|----------------------|--------------------|------------------------|
| field.id               | The ID of the field to set the enum option.                       | tag                  | <center>X</center> |                        |
| option.ids             | The IDs of the enum options to add to the field.                  | safety, installation | <center>X</center> | Comma-separated values |
| clear.multi.enum.field | Clear the multi enum field before adding the enum options         | false                |                    | Default value: true    |
| only.valid.options     | Throw an error if the enum option is not available for the field. | false                |                    | Default value: true    |


### SetRevisionOfWorkflowObjectHyperlinks

This function replaces the workitem, document, and testrun hyperlinks in the configured fields with hyperlinks pointing to their last revision. The function can be configured to not use the latest revision, but the revision of the last status change or to remove the revision from the links again.

The function currently only supports RichText style hyperlinks and not those created with the RenderingAPI. Furthermore, custom labels for hyperlinks are also not supported.

| Parameter                  | Description                                             | Example                   | Mandatory          | Comment                                                |
|----------------------------|---------------------------------------------------------|---------------------------|--------------------|--------------------------------------------------------|
| field.ids                  | The IDs of the fields containing the hyperlinks.        | description, summaryTable | <center>X</center> | Comma-separated values<br>Field Types: RichText, Table |
| use.last.status.transition | Use the revision of the last status transition.         | true                      |                    | Default value: false                                   |
| overwrite.revision         | Replace any already existing revision in the hyperlinks | true                      |                    | Default value: false                                   |
| remove.revision            | Remove any revision from the hyperlinks                 | true                      |                    | Default value: false                                   |

### StoreRevisionOfWorkflowObjectFromField

This function stores the revision of a work item, module (document), or test run in a field of the current object.
The function either uses the head revision of the other object, or the revision of the work item or module in the collection if one is specified in a field.

| Parameter           | Description                                                                  | Example           | Mandatory          | Comment                                          |
|---------------------|------------------------------------------------------------------------------|-------------------|--------------------|--------------------------------------------------|
| object.field.id     | The ID of the field containing the object.                                   | test_run          | <center>X</center> | Field Type: Single Enum                          |
| revision.field.id   | The ID of the field where the revision shall be stored                       | test_run_revision | <center>X</center> | Field Type: Integer                              |
| collection.field.id | The ID of the field where the collection is specified.                       | collection        |                    | Field Type: Single Enum, only relevant documents |
| status.id           | The ID of the status for who's last change the revision shall be determined. | passed            |                    | Not compatible with collection.field.id          |

# WorkItems

The following conditions and functions are only available for WorkItems.

## Conditions

### AllUsersSelectedInFieldApproved

This condition passes if all users selected in the fields defined in `field.ids` have approved the work item. Optionally, the condition throws an error if someone else not selected in the fields has approved.

| Parameter                   | Description                                                        | Example            | Mandatory          | Comment                                                 |
|-----------------------------|--------------------------------------------------------------------|--------------------|--------------------|---------------------------------------------------------|
| field.ids                   | The IDs of the fields containing the users.                        | reviewer, approver | <center>X</center> | Comma-separated values<br>Field Type: Single/multi Enum |
| ignore.additional.approvals | Ignores additional approvals or throws an error if there are some. | test_run_revision  |                    | Default value: false                                    |

### ContainedInDocument

This condition passes if the work item is contained in a module (document), optionally with the type and status specified in `module.type.ids` and `module.status.ids`.
The parameters can also be negated with the parameter `negate`.

| Parameter         | Description                                | Example                                                              | Mandatory | Comment                |
|-------------------|--------------------------------------------|----------------------------------------------------------------------|-----------|------------------------|
| module.type.ids   | The IDs of the permissible document types. | system_requirement_specification, software_requirement_specification |           | Comma-separated values |
| module.status.ids | The IDs of the permissible status          | draft, in_review                                                     |           | Comma-separated values |
| negate            | Negate the previous conditions.            | true                                                                 |           | Default value: false   |

### LinkedWorkItems

This condition combines and extends the two Polarion built-in workflow conditions LinkedWorkItem and LinkedWorkItemsStatus. While supporting all the use cases of the two conditions, it provides several more features:
- Require a minimum or maximum number of work item links matching the configuration.
- If the work item is not linked to HEAD but to a specific revision of the linked work item, the condition evaluates that revision instead of always using the HEAD revision.
- Optionally, the condition fails if a work item link is unresolvable.
- Optionally, the condition fails when work items not matching the configured target types and status are linked.
- Optionally, only active this condition if one or multiple field values match a regular expression (check the condition CheckFieldRegEx as a reference).

This condition is executed as system user to return correct results even if the user executing the workflow action does have access to all resources.

| Parameter                  | Description                                                                                                              | Example                        | Mandatory            | Comment                                                                                   |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------|--------------------------------|----------------------|-------------------------------------------------------------------------------------------|
| link.roles.direct          | The IDs of the direct link roles to check.                                                                               | parent, refines, relates_to    | <center>(X)</center> | Comma-separated values<br>Either link.roles.direct or link.roles.back need to be defined. |
| link.roles.back            | The IDs of the back link roles to check.                                                                                 | verifies, implements           | <center>(X)</center> | Comma-separated values<br>Either link.roles.direct or link.roles.back need to be defined. |
| target.workitem.type.ids   | The optional list of target work item type IDs the link target needs to fulfill.                                         | requirement, task              |                      | Comma-separated values                                                                    |
| target.workitem.status.ids | The optional list of target status IDs the link target needs to fulfill.                                                 | approved, complete             |                      | Comma-separated values                                                                    |
| min                        | The optional minimum number of required links matching the previous conditions.                                          | 2                              |                      | Default value: 1                                                                          |
| max                        | The optional maximum number of required links matching the previous conditions.                                          | 1                              |                      | Default value: The maximum integer value.                                                 |
| fail.for.unresolvable.links| The condition fails if one work item link matching the link roles is not resolvable.                                     | false                          |                      | Default value: true                                                                       |
| check.revision             | Instead of always checking the HEAD revision of the linked work item, the condition checks the actually linked revision. | true                           |                      | Default value: false                                                                      |
| target.type.only           | Optionally require all link targets with the defined link role to match the specified work item type.                    | true                           |                      | Default value: false                                                                      |
| target.status.only         | Optionally require all link targets with the defined link role to match the specified work item status.                  | true                           |                      | Default value: false                                                                      |
| regex.\<fieldId>           | One or many RegEx conditions that activate or deactivate this workflow condition.                                        | regex.control_category=testing |                      | See instructions for CheckFieldRegEx workflow condition, empty fields always pass.        |
| require.all.regex.pass     | Require all RegEx conditions to pass (true) or just at least one (false).                                                | true                           |                      | Default value: false                                                                      |

## Functions

### ExecuteLinkedWorkItemsAction

This function executes workflow actions of direct and/or backlinked work items. For an `action.id`, users can define permissible `origin.states` (or exclude these), the `target.state`, and `workitem.type.ids`.
The function can either `follow.links` or only execute the workflow action for the directly linked work items. Loops are detected and aborted with a `UserFriendlyRuntimeException`.

| Parameter         | Description                                                                                                                                                                                                                                                               | Example                | Mandatory            | Comment                                                                                                                                                                            |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| action.id         | The ID of the action to execute.                                                                                                                                                                                                                                          | send_to_review         | <center>X</center>   | If the action is generally available from the origin to the target status but blocked due to some conditions, a UserFriendlyRuntimeException is thrown, and the action is canceled. |
| link.roles.direct | The IDs of the direct link roles to the linked work items for which the action shall be executed.                                                                                                                                                                         | relates_to, implements | <center>(X)</center> | Comma-separated values<br>Either direct or back needs to be specified.                                                                                                             |
| link.roles.back   | The IDs of the back link roles to the linked work items for which the action shall be executed.                                                                                                                                                                           | parent                 | <center>(X)</center> | Comma-separated values<br>Either direct or back needs to be specified.                                                                                                             |
| origin.states     | The IDs of the permissible origin state of the linked work item for which the action shall be executed.                                                                                                                                                                   | draft, change          | <center>X</center>   | Comma-separated values                                                                                                                                                             |
| origin.negate     | Excludes the above specified `origin.states` and allows all others.                                                                                                                                                                                                       | false                  |                      | Default value: false                                                                                                                                                               |
| target.state      | The ID of the target state for the linked work item.                                                                                                                                                                                                                      | in_review              | <center>X</center>   |                                                                                                                                                                                    |
| workitem.type.ids | The IDs of the permissible work item types of the linked work item.                                                                                                                                                                                                       | task, requirement      |                      | Comma-separated values                                                                                                                                                             |
| follow.links      | If the parameter is set to true, the workflow action is executed not only for the directly linked Work Items, but also for their linked Work Items, and so on. Linked Work Item loops are caught and result in a UserFriendlyRuntimeException and the action is canceled. | true                   |                      | Default value: false                                                                                                                                                               |

### SetLinkedWorkItemsRevision

This function sets a pinned revision for the work item links matching the configuration parameters. The function supports three modes:
- Use the revision of the last status transition (optionally, of a specific status).
- Use the revision at the date/time specified by the date field of this work item.
- Use the latest revision of the linked work item if the date field is not mandatory.

The function ignores unresolvable work item links and displays an error message if the work item did not yet exist at the time specified in the date field.

| Parameter                     | Description                                                                                                            | Example                | Mandatory            | Comment                                                                                                      |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------|------------------------|----------------------|--------------------------------------------------------------------------------------------------------------|
| link.role.direct.ids          | The IDs of the link roles to process.                                                                                  | relates_to, refines    |                      | Comma-separated values<br>If the parameter is not defined, the function does not filter by link role ID.     |
| workitem.target.type.ids      | The type IDs of the linked work items.                                                                                 | requirement, test_case |                      | Comma-separated values<br>If the parameter is not defined, the function does not filter by work item type.   |
| workitem.target.status.ids    | The status IDs of the linked work items.                                                                               | draft, open            |                      | Comma-separated values<br>If the parameter is not defined, the function does not filter by work item status. |
| date.field.id                 | The ID of the date field.                                                                                              | execution_date         | <center>(X)</center> | Field Type:Date, DateOnly<br>Only mandatory if `always.use.revision.from.date` is true.                      |
| always.use.revision.from.date | Make the date field mandatory. Otherwise, the latest revision of the linked work item is used when the field is empty. | true                   |                      | Default value: false, cannot be true when use.last.status.transition is true                                 |
| use.last.status.transition    | Take the revision of the last status transition.                                                                       | true                   |                      | Default value: false, cannot be true when always.use.revision.from.date is true                              |
| require.status.transition     | Throw an error if the status transition has not occurred yet.                                                          | true                   |                      | Default value: false                                                                                         |
| transition.status.id          | Take the revision of the last transition to the status with this ID.                                                   | approved               |                      | Only relevant when use.last.status.transition is true                                                        |

### UpdateSuspectFlag

This function adds or removes the suspect flag for all linked work items matching the parameters. The function always uses the system user to retrieve all work item links.
To bypass write permission restrictions when changing the suspect flag, the function can `use.system.user`.
However, the function still fails if the user does not have write access to the backlinked work item at all.

| Parameter         | Description                                                                                              | Example                | Mandatory | Comment                |
|-------------------|----------------------------------------------------------------------------------------------------------|------------------------|-----------|------------------------|
| workitem.type.ids | The IDs of the linked work item types. If not specified, the function is applied to all work item types. | requirement, test_case |           | Comma-separated values |
| link.roles.direct | The optional list of direct link role IDs for which the action shall be executed.                        | relates_to, implements |           | Comma-separated values |
| link.roles.back   | The optional list of back link role IDs which the action shall be executed.                              | relates_to, implements |           | Comma-separated values |
| set.suspect       | Activates or deactivates the suspect flag.                                                               | true                   |           | Default value: true    |
| use.system.user   | Use System User to override permission settings. The action is still executed as the same user.          | true                   |           | Default value: false   |

# Modules (Documents)

The following conditions and functions are only available for Modules (Documents).

## Conditions

### ContainsMatchingWorkItems

This condition passes if the module (document) contains work items matching the `query`. Users can specify a `minimum` or `maximum` amount of work items and `exclude.referenced.items` or choose to consider `exclusively.referenced.items`. Similar to the built-in workflow condition `ContainsNoMatchingWorkItems`, it cannot account for changes to the work items in parallel to the workflow action. The benefit of this custom implementation is that it detects deleted or added work items and requires the user to save the document first without performing the workflow action.

| Parameter                    | Description                                          | Example                   | Mandatory          | Comment                                   |
|------------------------------|------------------------------------------------------|---------------------------|--------------------|-------------------------------------------|
| query                        | The query to specify the work items.                 | type:(system_requirement) | <center>X</center> |                                           |
| exclude.referenced.items     | Excludes external / referenced items from the query. | true                      |                    | Default value: false                      |
| exclusively.referenced.items | Only check for external / referenced work items.     | false                     |                    | Default value: false                      |
| minimum                      | The minimum number of work items.                    | 5                         |                    | Default value: 1                          |
| maximum                      | The maximum number of work items.                    | 10                        |                    | Default value: The maximum integer value. |

### ContainsString

This condition passes if the module (document) itself, so the content not included in work items, contains one or all the `strings`.
The condition can also invert the behavior with the `block` parameter.
This condition can be used to prevent users from creating empty documents and instead force them to always reuse of the templates.

| Parameter   | Description                                                                                                     | Example                                               | Mandatory          | Comment                |
|-------------|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------|--------------------|------------------------|
| strings     | The strings that must be included in the document.                                                              | #coverSheet($document), #glossary($document)          | <center>X</center> | Comma-separated values |
| require.all | Require all strings to be found (or not found) in the document content.                                         | true                                                  |                    | Default value: false   |
| block       | Block the workflow condition if the sting(s) are found.                                                         | true                                                  |                    | Default value: false   |
| message     | The optional additional error message that is appended to the automatic error message when the condition fails. | Remember to always reuse from the document templates. |                    |                        |

### RecycleBinIsEmpty

This condition passes if the document's recycle bin does not contain any work items.

| Parameter | Description | Example | Mandatory | Comment |
|-----------|-------------|---------|-----------|---------|
| N/A       |             |         |           |         |

### TestCasesHaveTestRecords

This condition passes if all contained work items (test cases) in the document matching the configured type and status have the amount of test records based on the condition's configuration.

This condition is executed as system user to return correct results even if the user executing the workflow action does have access to all resources.

| Parameter                         | Description                                                                                                                                                        | Example                              | Mandatory | Comment                                                                                                       |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|-----------|---------------------------------------------------------------------------------------------------------------|
| workitem.type.ids                 | The type IDs of the work items to be checked.                                                                                                                      | system_test_case, software_test_case |           | Comma-separated values<br>If the parameter is not defined, the condition does not filter by work item type.   |
| workitem.status.ids               | The status IDs of the work items to be checked.                                                                                                                    | in_review, approved                  |           | Comma-separated values<br>If the parameter is not defined, the condition does not filter by work item status. |
| testrecord.result.ids             | The result IDs of valid test records.                                                                                                                              | passed                               |           | Comma-separated values<br>If the parameter is not defined, all test result types are considered.              |
| testrun.type.ids                  | The type IDs of valid test runs.                                                                                                                                   | manual, automatic                    |           | Comma-separated values<br>If the parameter is not defined, all test run types are considered.                 |
| testrun.status.ids                | The status IDs of valid test runs.                                                                                                                                 | passed                               |           | Comma-separated values<br>If the parameter is not defined, all test run status are considered.                |
| include.referenced                | Also check those work items that are not contained but referenced in the document.                                                                                 | true                                 |           | Default value: true                                                                                           |
| scope.project                     | Only consider test runs in the same project as the document.                                                                                                       | true                                 |           | Default value: false                                                                                          |
| since.last.status.transition.only | Only consider test records that were executed for the revision of the work item's last status transition or later. Work Item creation counts as status transition. | true                                 |           | Default value: false                                                                                          |
| min                               | The minimum required number of test records matching the above configuration.                                                                                      | 1                                    |           | Default value: 1                                                                                              |
| max                               | The maximum allowed number of test records matching the above configuration.                                                                                       | 1                                    |           | Default value: maximum integer                                                                                |
| search.limit                      | The maximum number of test records to check for each test case.                                                                                                    | 100                                  |           | Default value: 999                                                                                            |

### WorkItemsHaveLinkedWorkItems

This condition passes if all work items in the document matching the configured type and status have the amount of work item links based on the condition's configuration. The number of found direct and back links are accumulated when comparing against the min and max threshold. If you want to make sure the work item has a certain amount of direct links and a certain amount of back links matching the configuration, you need to configure two separate workflow conditions, one for the direct links and one for the back links.

This condition is executed as system user to return correct results even if the user executing the workflow action does have access to all resources.

| Parameter                  | Description                                                                        | Example                                  | Mandatory            | Comment                                                                                                                                                  |
|----------------------------|------------------------------------------------------------------------------------|------------------------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| workitem.source.type.ids   | The type IDs of the work items to be checked.                                      | system_requirement, software_requirement |                      | Comma-separated values<br>If the parameter is not defined, the condition does not filter by work item type.                                              |
| workitem.source.status.ids | The status IDs of the work items to be checked.                                    | in_review, approved                      |                      | Comma-separated values<br>If the parameter is not defined, the condition does not filter by work item status.                                            |
| linkrole.direct.ids        | The IDs of the direct link roles to check.                                         | parent, refines                          | <center>(X)</center> | Comma-separated values<br>If the parameter is not defined, the condition does not check direct links. Either direct or back link IDs need to be defined. |
| linkrole.back.ids          | The IDs of the back link roles to check.                                           | verifies, validates                      | <center>(X)</center> | Comma-separated values<br>If the parameter is not defined, the condition does not check back links. Either direct or back link IDs need to be defined.   |
| workitem.target.type.ids   | The type IDs of valid link targets.                                                | test_case, system_requirement            |                      | Comma-separated values<br>If the parameter is not defined, every target work item type is considered.                                                    |
| workitem.target.status.ids | The status IDs of valid link targets.                                              | checked, approved                        |                      | Comma-separated values<br>If the parameter is not defined, every target work item status is considered.                                                  |
| include.referenced         | Also check those work items that are not contained but referenced in the document. | true                                     |                      | Default value: true                                                                                                                                      |
| ignore.suspect.links       | Ignore work item links that are suspected.                                         | false                                    |                      | Default value: false                                                                                                                                     |
| scope.project              | Only consider linked work items in the same project as the document.               | true                                     |                      | Default value: false                                                                                                                                     |
| min                        | The minimum required number of work item links matching the above configuration.   | 1                                        |                      | Default value: 1                                                                                                                                         |
| max                        | The maximum allowed number of work item links matching the above configuration.    | 1                                        |                      | Default value: maximum integer                                                                                                                           |

## Functions

### AddEnumOptionsFromWorkItems

This function copies the enum values from one or many fields of the internal or referenced work items in the document matching the `query` to a document enum field. The function can optionally remove all preexisting entries in the document field and check the num values to be valid for the document field. The function skips work items that do not have fields specified in `workitem.field.ids`. The function throws an error if the configured document or work item fields are not enum fields.

| Parameter                  | Description                                                                   | Example            | Mandatory          | Comment                           |
|----------------------------|-------------------------------------------------------------------------------|--------------------|--------------------|-----------------------------------|
| query                      | The optional query to filter the work items to take the enum values from.     | status:approved    |                    | Default value: NOT type:(heading) |
| include.internal.workitems | Include the work items contained in the document.                             | true               |                    | Default value: true               |
| include.external.workitems | Include the external work items referenced in the document.                   | false              |                    | Default value: true               |
| document.field.id          | The ID of the document multi-select enum field.                               | signers            | <center>X</center> |                                   |
| workitem.field.ids         | The IDs of the work item enum fields.                                         | reviewer, approver | <center>X</center> | Comma-separated values            |
| clear.document.field       | Clear the document enum field before adding the enum options                  | false              |                    | Default value: false              |
| only.valid.options         | Throw an error if one of the options is not available for the document field. | false              |                    | Default value: true               |


### EmptyRecycleBin

This function empties the document's recycle bin, optionally with admin privileges to circumvent user permissions.
However, the function still fails if the user does not have write access to the work items at all.

| Parameter       | Description                                                                                     | Example | Mandatory | Comment              |
|-----------------|-------------------------------------------------------------------------------------------------|---------|-----------|----------------------|
| use.system.user | Use System User to override permission settings. The action is still executed as the same user. | true    |           | Default value: false |

### UpdateReferencedWorkItems

This function updates (freezes) the referenced work items of the module (document) to their latest revision.
If the referenced work items belong to a document in the same collection as the document of the workflow action, the revision of the work items in that collection is used.
This can be overruled by the parameter `disregard.collection`.

| Parameter            | Description                                                                                 | Example | Mandatory | Comment              |
|----------------------|---------------------------------------------------------------------------------------------|---------|-----------|----------------------|
| disregard.collection | Always use getLastRevision() of work items even when the work item belongs to a collection. | true    |           | Default value: false |

### UpdateTitleWorkItemWithDocumentTitle

This function updates the document's title work item with the title of the document itself. If no title is specified, it uses the name (ID) of the document. The function does nothing if the document does not have a title work item.

| Parameter | Description | Example | Mandatory | Comment |
|-----------|-------------|---------|-----------|---------|
| N/A       |             |         |           |         |


# TestRuns

The following conditions and functions are only available for TestRuns.

## Conditions

### AllTestRecordsExecuted

This condition checks if all test records of the test run have been executed. Optionally, the test records must have one of the specified result IDs.

| Parameter        | Description                                                   | Example        | Mandatory | Comment                                                                                         |
|------------------|---------------------------------------------------------------|----------------|-----------|-------------------------------------------------------------------------------------------------|
| valid.result.ids | The IDs of the test record results to be considered executed. | passed, failed |           | The executed test records are not filtered for the results, if this parameter is not specified. |

## Functions

### AddTestCasesFromCollection

With Polarion OOTB features up until Polarion 2310, test cases can only be added to a test run in the HEAD revision, or the revision as contained in a module (document). Polarion 2404 allows to manually select the document revision, however, there is no support to select the revision of a document as contained in a collection.

This function adds the test cases of a module (document) specified in the field `document.field.id` in the revision of the collection specified in the field `collection.field.id`.
When using the Delta-Mode based on the value of `delta.mode.field.id`, the function only adds test cases that need retesting. Test Cases need to be retested when the work item revision has changed, or one of the enum fields in the old test run defined in `compare.enum.field.ids` contained values (e.g. users, test parameters) not selected in the current test run.
The function can apply a `testrun.status.filter` and a `testrecord.status.filter` when searching for existing test records. 

| Parameter                | Description                                                                      | Example            | Mandatory          | Comment                                                 |
|--------------------------|----------------------------------------------------------------------------------|--------------------|--------------------|---------------------------------------------------------|
| collection.field.id      | The field ID where the collection is specified.                                  | collection         | <center>X</center> | Field Type: Single Enum                                 |
| document.field.id        | The field ID where the document is specified.                                    | document           | <center>X</center> | Field Type: Single Enum                                 |
| delta.mode.field.id      | The field ID where the delta mode is specified                                   | delta_mode         |                    | Field Type: Boolean                                     |
| compare.enum.field.ids   | The IDs of the enum fields to compare for the delta mode.                        | student, parameter |                    | Comma-separated values<br>Field Type: Single/Multi Enum |
| testrun.status.filter    | The status IDs of the existing test runs to consider for the delta mode.         | passed, verified   |                    | Comma-separated values                                  |
| testrecord.status.filter | The status IDs of the existing test records to consider for the delta mode.      | passed             |                    | Comma-separated values                                  |
| test.records.limit       | The maximum number of test records of a test case to check for previous results. | 1000               |                    | Default value: 999,999                                  |

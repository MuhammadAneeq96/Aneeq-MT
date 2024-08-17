# Changelog

## 1.25.0

### Features

- updated Work Item Condition LinkedworkItems
  - optionally fail if a linked work item is not resolvable
  - optionally define one or many field regular expressions to activate this condition
  - optionally check the specifically linked revision of the linked work item and not always the HEAD revision
- updated Work Item Function SetLinkedWorkItemsRevision
  -   optionally set the work item link to the revision of the last status transition of the linked work item
- added Generic Condition UserSelectedInFieldHasRole
package com.tulrfsd.polarion.workflows.generic.functions;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import com.tulrfsd.polarion.core.utils.WorkflowObjectCoreHelper;
import com.tulrfsd.polarion.workflows.utils.Literals;

public class StoreRevisionOfWorkflowObjectFromField implements IFunction<IWorkflowObject> {
  
  static final String FUNCTION_NAME = Literals.WORKFLOW_PREFIX.getValue() + StoreRevisionOfWorkflowObjectFromField.class.getSimpleName();

  @Override
  public void execute(ICallContext<IWorkflowObject> context, IArguments arguments) {
    
    // read parameters
    final String collectionFieldId = arguments.getAsString("collection.field.id", "");
    final String statusId = arguments.getAsString("status.id", "");
    final String objectFieldId = arguments.getAsString("object.field.id");
    final String revisionFieldId = arguments.getAsString("revision.field.id");
    
    IWorkflowObject object = context.getTarget();
    checkParameters(collectionFieldId, statusId, objectFieldId, revisionFieldId, object);
    
    IPObject enumObject = object.getDataSvc().getObjectFromEnumOption((IEnumOption) object.getValue(objectFieldId));
    if (enumObject == null || enumObject.isUnresolvable()) {
      throw new UserFriendlyRuntimeException(String.format("The field %s is empty or contains an unresolvable object.", objectFieldId));
    } else if (!(enumObject instanceof IWorkflowObject)) {
      throw new UserFriendlyRuntimeException(String.format("The field %s must contain a work item, document or test run.", objectFieldId));
    }
    
    String revision = enumObject.getRevision() == null ? enumObject.getLastRevision() : enumObject.getRevision();
    
    if (!collectionFieldId.isEmpty()) {
      if (enumObject instanceof IModule) {
        IBaselineCollection collection = WorkflowObjectCoreHelper.getCollectionFromEnumField( object, collectionFieldId);
        IModule module = WorkflowObjectCoreHelper.getModuleFromEnumFieldAndCollection(object, objectFieldId, collection);
        revision = module.getRevision() == null ? module.getDataRevision() : module.getRevision();
      } else {
        throw new IllegalArgumentException(String.format("The parameter collection.field.id is only compatible with documents in the enum field. Please check the %s's workflow function %s.", object.getPrototype().getName(), FUNCTION_NAME));
      }
    } else if (!statusId.isEmpty()) {
      revision = WorkflowObjectCoreHelper.getRevisionOfLastStatusChange((IWorkflowObject) enumObject, statusId);
    }

    object.setValue(revisionFieldId, Integer.valueOf(revision));
    
  }

  private void checkParameters(final String collectionFieldId, final String statusId, final String objectFieldId, final String revisionFieldId, IWorkflowObject object) {
    if (!collectionFieldId.isEmpty() && !statusId.isEmpty()) {
      throw new IllegalArgumentException(String.format("The parameters collection.field.id and status.id cannot be used at the same time. Please check the %s's workflow function %s.", object.getPrototype().getName(), FUNCTION_NAME));
    } else if (!WorkflowObjectCoreHelper.isSingleEnumField(object, objectFieldId)) {
      throw new IllegalArgumentException(String.format("The field %s must be an enumeration field specifying a single workitem, document or test run. Please check the %s's workflow function %s if this is specified in a different field.", objectFieldId, object.getPrototype().getName(), FUNCTION_NAME));
    } else if (!collectionFieldId.isEmpty() && !WorkflowObjectCoreHelper.isSingleEnumField(object, collectionFieldId)) {
      throw new IllegalArgumentException(String.format("The field %s must be an enumeration field specifying a single collection. Please check the %s's workflow function %s if this is specified in a different field.", collectionFieldId, object.getPrototype().getName(), FUNCTION_NAME));
    } else if (!WorkflowObjectCoreHelper.isIntegerField(object, revisionFieldId)) {
      throw new IllegalArgumentException(String.format("The field %s must be an Integer field. Please check the %s's workflow function %s if this is specified in a different field.", revisionFieldId, object.getPrototype().getName(), FUNCTION_NAME));
    }
  }

}

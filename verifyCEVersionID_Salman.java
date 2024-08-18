package com.tulrfsd.polarion.workflows.workitems.conditions;

import com.tulrfsd.polarion.core.model.ICustomWorkflowCondition;


public class verifyCEVersionID implements ICustomWorkflowCondition<IWorkItem>{

    static final String CONDITION_NAME = Literals.WORKFLOW_PREFIX.getValue() + LinkedWorkItems.class.getSimpleName();

    @Override
    public String checkCondition(ICallContext<IWorkItem> context, IArguments arguments){



        
    }

}
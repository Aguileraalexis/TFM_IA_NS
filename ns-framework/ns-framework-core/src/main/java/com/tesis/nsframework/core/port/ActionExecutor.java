package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.ExecutionContext;
import com.tesis.nsframework.core.model.PlannedAction;

public interface ActionExecutor {
    ActionOutcome execute(PlannedAction action, ExecutionContext context);
}

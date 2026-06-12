package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.model.SymbolicState;

public interface StateUpdater {
    SymbolicState update(SymbolicState currentState, PlannedAction action, ActionOutcome outcome);
}

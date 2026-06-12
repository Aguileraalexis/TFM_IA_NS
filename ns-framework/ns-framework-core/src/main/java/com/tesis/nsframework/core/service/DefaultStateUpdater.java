package com.tesis.nsframework.core.service;

import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.StateUpdater;

public class DefaultStateUpdater implements StateUpdater {
    @Override
    public SymbolicState update(SymbolicState currentState, PlannedAction action, ActionOutcome outcome) {
        SymbolicState newState = currentState.copy();
        if (outcome.success()) {
            outcome.observedEffects().forEach((fact, value) -> {
                if (Boolean.parseBoolean(value)) {
                    newState.addFact(fact);
                } else {
                    newState.removeFact(fact);
                }
            });
        }
        return newState;
    }
}

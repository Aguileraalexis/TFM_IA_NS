package com.tesis.nsframework.core.service;

import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.StateStore;

public class InMemoryStateStore implements StateStore {
    private SymbolicState state;

    public InMemoryStateStore(SymbolicState initialState) {
        this.state = initialState == null ? new SymbolicState() : initialState;
    }

    @Override
    public synchronized SymbolicState loadCurrentState() {
        return state.copy();
    }

    @Override
    public synchronized void save(SymbolicState state) {
        this.state = state.copy();
    }
}

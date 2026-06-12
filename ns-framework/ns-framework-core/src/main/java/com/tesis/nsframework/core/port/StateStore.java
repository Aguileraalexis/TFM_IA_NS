package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.SymbolicState;

public interface StateStore {
    SymbolicState loadCurrentState();
    void save(SymbolicState state);
}

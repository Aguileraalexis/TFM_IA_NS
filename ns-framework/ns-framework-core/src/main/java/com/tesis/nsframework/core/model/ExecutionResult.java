package com.tesis.nsframework.core.model;

import java.util.List;

public record ExecutionResult(
        boolean success,
        String message,
        List<String> executedActions,
        int replans,
        SymbolicState finalState
) {
    public ExecutionResult {
        executedActions = executedActions == null ? List.of() : List.copyOf(executedActions);
    }

    public static ExecutionResult success(String message, List<String> executedActions, int replans, SymbolicState finalState) {
        return new ExecutionResult(true, message, executedActions, replans, finalState);
    }

    public static ExecutionResult failure(String message, List<String> executedActions, int replans, SymbolicState finalState) {
        return new ExecutionResult(false, message, executedActions, replans, finalState);
    }
}

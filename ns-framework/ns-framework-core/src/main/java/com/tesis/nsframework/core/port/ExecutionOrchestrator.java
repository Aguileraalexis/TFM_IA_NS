package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.ExecutionResult;

public interface ExecutionOrchestrator {
    ExecutionResult run(String userInput);
}

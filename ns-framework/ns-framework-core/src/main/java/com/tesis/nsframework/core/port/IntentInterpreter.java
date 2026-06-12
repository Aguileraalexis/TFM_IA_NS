package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.InterpretationResult;

public interface IntentInterpreter {
    InterpretationResult interpret(String userInput, DomainMetadata domainMetadata);
}

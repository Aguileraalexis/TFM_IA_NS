package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.InterpretationResult;

public interface GoalMapper {
    GoalSpec map(InterpretationResult interpretationResult, DomainMetadata domainMetadata);
}

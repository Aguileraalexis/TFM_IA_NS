package com.tesis.nsframework.core.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record PlanningProblem(
        String problemName,
        String domainName,
        Set<String> objects,
        Set<String> initFacts,
        String goalExpression,
        Map<String, Object> metadata
) {
    public PlanningProblem {
        objects = objects == null ? Set.of() : Set.copyOf(objects);
        initFacts = initFacts == null ? Set.of() : Set.copyOf(initFacts);
        metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }
}

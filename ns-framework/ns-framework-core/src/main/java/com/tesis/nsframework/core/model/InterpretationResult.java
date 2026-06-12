package com.tesis.nsframework.core.model;

import java.util.Collections;
import java.util.Map;

public record InterpretationResult(
        String intent,
        Map<String, Object> entities,
        Map<String, Object> constraints,
        Double confidence,
        String rawResponse
) {
    public InterpretationResult {
        entities = entities == null ? Collections.emptyMap() : Collections.unmodifiableMap(entities);
        constraints = constraints == null ? Collections.emptyMap() : Collections.unmodifiableMap(constraints);
    }
}

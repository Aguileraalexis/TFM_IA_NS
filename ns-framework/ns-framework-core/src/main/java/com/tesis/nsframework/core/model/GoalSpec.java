package com.tesis.nsframework.core.model;

import java.util.Collections;
import java.util.Map;

public record GoalSpec(
        String goalPredicate,
        Map<String, String> parameters,
        String domainName,
        String description
) {
    public GoalSpec {
        parameters = parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(parameters);
    }
}

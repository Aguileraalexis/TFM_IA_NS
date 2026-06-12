package com.tesis.nsframework.core.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record DomainMetadata(String domainName, Set<String> supportedIntents, Map<String, String> intentToGoalPredicate) {
    public DomainMetadata {
        supportedIntents = supportedIntents == null ? Set.of() : Set.copyOf(supportedIntents);
        intentToGoalPredicate = intentToGoalPredicate == null ? Collections.emptyMap() : Collections.unmodifiableMap(intentToGoalPredicate);
    }

    public boolean supportsIntent(String intent) {
        return supportedIntents.contains(intent);
    }
}

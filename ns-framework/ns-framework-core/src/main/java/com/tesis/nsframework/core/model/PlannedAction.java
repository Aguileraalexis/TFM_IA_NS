package com.tesis.nsframework.core.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record PlannedAction(
        String name,
        List<String> arguments,
        Map<String, Object> metadata
) {
    public PlannedAction {
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    public String asInvocation() {
        return name + "(" + String.join(", ", arguments) + ")";
    }
}

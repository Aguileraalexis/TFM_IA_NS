package com.tesis.nsframework.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ExecutionContext {
    private final Map<String, Object> variables = new HashMap<>();

    public ExecutionContext() {
    }

    public ExecutionContext(Map<String, Object> initialVariables) {
        if (initialVariables != null) {
            variables.putAll(initialVariables);
        }
    }

    public Object get(String key) {
        return variables.get(key);
    }

    public String getAsString(String key) {
        Object value = variables.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public void put(String key, Object value) {
        variables.put(key, value);
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(variables);
    }
}

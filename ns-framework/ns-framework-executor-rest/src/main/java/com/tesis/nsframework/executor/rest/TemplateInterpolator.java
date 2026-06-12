package com.tesis.nsframework.executor.rest;

import com.tesis.nsframework.core.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateInterpolator {
    public Map<String, Object> interpolateMap(Map<String, Object> template, ExecutionContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        template.forEach((key, value) -> output.put(key, interpolateValue(value, context)));
        return output;
    }

    public Object interpolateValue(Object value, ExecutionContext context) {
        if (value instanceof String stringValue) {
            String result = stringValue;
            for (Map.Entry<String, Object> entry : context.asMap().entrySet()) {
                result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
            return result;
        }
        return value;
    }
}

package com.tesis.nsframework.executor.rest;

public class SuccessConditionEvaluator {
    public boolean isSuccessful(String successCondition, int statusCode) {
        if (successCondition == null || successCondition.isBlank()) {
            return statusCode >= 200 && statusCode < 300;
        }
        String trimmed = successCondition.replace(" ", "");
        if (trimmed.startsWith("status==")) {
            int expected = Integer.parseInt(trimmed.substring("status==".length()));
            return expected == statusCode;
        }
        if (trimmed.equals("status2xx")) {
            return statusCode >= 200 && statusCode < 300;
        }
        return false;
    }
}

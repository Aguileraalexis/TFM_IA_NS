package com.tesis.nsframework.core.service;

import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.port.GoalMapper;

import java.util.HashMap;
import java.util.Map;

public class DefaultGoalMapper implements GoalMapper {
    @Override
    public GoalSpec map(InterpretationResult interpretationResult, DomainMetadata domainMetadata) {
        if (interpretationResult == null || interpretationResult.intent() == null || interpretationResult.intent().isBlank()) {
            throw new FrameworkException("Intent is required to build a goal");
        }
        if (!domainMetadata.supportsIntent(interpretationResult.intent())) {
            throw new FrameworkException("Unsupported intent: " + interpretationResult.intent());
        }

        String goalPredicate = domainMetadata.intentToGoalPredicate().get(interpretationResult.intent());
        Map<String, String> params = new HashMap<>();
        interpretationResult.entities().forEach((k, v) -> params.put(k, String.valueOf(v)));
        interpretationResult.constraints().forEach((k, v) -> params.putIfAbsent(k, String.valueOf(v)));

        return new GoalSpec(goalPredicate, params, domainMetadata.domainName(), "Goal generated from interpreted intent");
    }
}

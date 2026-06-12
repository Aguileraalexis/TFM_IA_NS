package com.tesis.nsframework.core.service;

import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.PlanningProblemBuilder;

import java.util.Set;
import java.util.stream.Collectors;

public class DefaultPlanningProblemBuilder implements PlanningProblemBuilder {
    @Override
    public PlanningProblem build(SymbolicState state, GoalSpec goalSpec) {
        Set<String> objects = goalSpec.parameters().values().stream().collect(Collectors.toSet());
        String goalExpression = "(" + goalSpec.goalPredicate() + " " + String.join(" ", goalSpec.parameters().values()) + ")";
        return new PlanningProblem(
                "generated-problem",
                goalSpec.domainName(),
                objects,
                state.facts(),
                goalExpression,
                goalSpec.parameters().entrySet().stream().collect(Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue))
        );
    }
}

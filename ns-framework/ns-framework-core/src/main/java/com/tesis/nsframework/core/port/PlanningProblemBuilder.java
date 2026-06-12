package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.model.SymbolicState;

public interface PlanningProblemBuilder {
    PlanningProblem build(SymbolicState state, GoalSpec goalSpec);
}

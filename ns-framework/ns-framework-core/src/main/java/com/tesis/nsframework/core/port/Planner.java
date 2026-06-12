package com.tesis.nsframework.core.port;

import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;

public interface Planner {
    PlanResult plan(String domainPddl, PlanningProblem problem, PlannerOptions options);
}

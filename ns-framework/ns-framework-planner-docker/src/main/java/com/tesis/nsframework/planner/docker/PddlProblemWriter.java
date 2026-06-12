package com.tesis.nsframework.planner.docker;

import com.tesis.nsframework.core.model.PlanningProblem;

import java.util.StringJoiner;

public class PddlProblemWriter {
    public String write(PlanningProblem problem) {
        StringJoiner objectsJoiner = new StringJoiner("\n    ", "(:objects\n    ", "\n  )");
        problem.objects().forEach(objectsJoiner::add);

        StringJoiner initJoiner = new StringJoiner("\n    ", "(:init\n    ", "\n  )");
        problem.initFacts().forEach(initJoiner::add);

        return "(define (problem " + problem.problemName() + ")\n" +
                "  (:domain " + problem.domainName() + ")\n" +
                "  " + objectsJoiner + "\n" +
                "  " + initJoiner + "\n" +
                "  (:goal\n    " + problem.goalExpression() + "\n  )\n" +
                ")\n";
    }
}

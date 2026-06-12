package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.ExecutionContext;
import com.tesis.nsframework.core.model.ExecutionResult;
import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.ActionExecutor;
import com.tesis.nsframework.core.port.ExecutionOrchestrator;
import com.tesis.nsframework.core.port.GoalMapper;
import com.tesis.nsframework.core.port.IntentInterpreter;
import com.tesis.nsframework.core.port.Planner;
import com.tesis.nsframework.core.port.PlanningProblemBuilder;
import com.tesis.nsframework.core.port.StateStore;
import com.tesis.nsframework.core.port.StateUpdater;
import com.tesis.nsframework.core.model.DomainMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TravelExecutionOrchestrator implements ExecutionOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TravelExecutionOrchestrator.class);

    private final IntentInterpreter intentInterpreter;
    private final GoalMapper goalMapper;
    private final PlanningProblemBuilder planningProblemBuilder;
    private final Planner planner;
    private final ActionExecutor actionExecutor;
    private final StateUpdater stateUpdater;
    private final StateStore stateStore;
    private final DomainMetadata domainMetadata;
    private final TravelDomainPddlGenerator domainPddlGenerator;
    private final PlannerOptions plannerOptions;

    public TravelExecutionOrchestrator(IntentInterpreter intentInterpreter,
                                       GoalMapper goalMapper,
                                       PlanningProblemBuilder planningProblemBuilder,
                                       Planner planner,
                                       ActionExecutor actionExecutor,
                                       StateUpdater stateUpdater,
                                       StateStore stateStore,
                                       DomainMetadata domainMetadata,
                                       TravelDomainPddlGenerator domainPddlGenerator,
                                       PlannerOptions plannerOptions) {
        this.intentInterpreter = intentInterpreter;
        this.goalMapper = goalMapper;
        this.planningProblemBuilder = planningProblemBuilder;
        this.planner = planner;
        this.actionExecutor = actionExecutor;
        this.stateUpdater = stateUpdater;
        this.stateStore = stateStore;
        this.domainMetadata = domainMetadata;
        this.domainPddlGenerator = domainPddlGenerator;
        this.plannerOptions = plannerOptions == null ? PlannerOptions.defaults() : plannerOptions;
    }

    @Override
    public ExecutionResult run(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            throw new FrameworkException("User input must not be blank");
        }

        InterpretationResult interpretation = intentInterpreter.interpret(userInput, domainMetadata);
        GoalSpec goalSpec = goalMapper.map(interpretation, domainMetadata);
        SymbolicState state = new SymbolicState();
        stateStore.save(state);
        ExecutionContext executionContext = new ExecutionContext(interpretation.entities());
        interpretation.constraints().forEach(executionContext::put);
        List<String> executedActions = new ArrayList<>();

        String domainPddl = domainPddlGenerator.generate();
        PlanningProblem problem = planningProblemBuilder.build(state, goalSpec);
        // Enrich the execution context with planning metadata (airline routes, hotels, dates)
        // so the action executor can resolve all parameters without relying on action-level metadata
        // (which DockerPlanner does not populate — it only parses action name and arguments).
        problem.metadata().forEach(executionContext::put);
        PlanResult planResult = planner.plan(domainPddl, problem, plannerOptions);
        if (!planResult.success() || planResult.actions().isEmpty()) {
            LOGGER.warn("No valid travel plan found: {}", planResult.message());
            return ExecutionResult.failure(planResult.message(), executedActions, 0, state);
        }

        for (PlannedAction plannedAction : planResult.actions()) {
            LOGGER.info("Executing travel action {}", plannedAction.asInvocation());
            ActionOutcome outcome = actionExecutor.execute(plannedAction, executionContext);
            executedActions.add(plannedAction.asInvocation());
            state = stateUpdater.update(state, plannedAction, outcome);
            stateStore.save(state);

            if (!outcome.success()) {
                return ExecutionResult.failure(outcome.message(), executedActions, 0, state);
            }
        }

        return ExecutionResult.success("Travel execution completed successfully", executedActions, 0, state);
    }
}


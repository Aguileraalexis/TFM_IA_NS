package com.tesis.nsframework.core.service;

import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.*;
import com.tesis.nsframework.core.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultExecutionOrchestrator implements ExecutionOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExecutionOrchestrator.class);

    private final IntentInterpreter intentInterpreter;
    private final GoalMapper goalMapper;
    private final PlanningProblemBuilder problemBuilder;
    private final Planner planner;
    private final ActionExecutor actionExecutor;
    private final StateUpdater stateUpdater;
    private final StateStore stateStore;
    private final DomainMetadata domainMetadata;
    private final String domainPddl;
    private final PlannerOptions plannerOptions;

    public DefaultExecutionOrchestrator(IntentInterpreter intentInterpreter,
                                        GoalMapper goalMapper,
                                        PlanningProblemBuilder problemBuilder,
                                        Planner planner,
                                        ActionExecutor actionExecutor,
                                        StateUpdater stateUpdater,
                                        StateStore stateStore,
                                        DomainMetadata domainMetadata,
                                        String domainPddl,
                                        PlannerOptions plannerOptions) {
        this.intentInterpreter = intentInterpreter;
        this.goalMapper = goalMapper;
        this.problemBuilder = problemBuilder;
        this.planner = planner;
        this.actionExecutor = actionExecutor;
        this.stateUpdater = stateUpdater;
        this.stateStore = stateStore;
        this.domainMetadata = domainMetadata;
        this.domainPddl = domainPddl;
        this.plannerOptions = plannerOptions == null ? PlannerOptions.defaults() : plannerOptions;
    }

    @Override
    public ExecutionResult run(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            throw new FrameworkException("User input must not be blank");
        }

        InterpretationResult interpretation = intentInterpreter.interpret(userInput, domainMetadata);
        GoalSpec goalSpec = goalMapper.map(interpretation, domainMetadata);
        SymbolicState state = stateStore.loadCurrentState();
        ExecutionContext executionContext = new ExecutionContext(interpretation.entities());
        interpretation.constraints().forEach(executionContext::put);
        List<ExecutionResult.ExecutedAction> executedActions = new ArrayList<>();
        int replans = 0;

        while (true) {
            PlanningProblem problem = problemBuilder.build(state, goalSpec);
            PlanResult planResult = planner.plan(domainPddl, problem, plannerOptions);

            if (!planResult.success() || planResult.actions().isEmpty()) {
                LOGGER.warn("No se encontro un plan valido: {}", planResult.message());
                return ExecutionResult.failure(planResult.message(), executedActions, replans, state);
            }

            boolean replanNeeded = false;
            for (PlannedAction plannedAction : planResult.actions()) {
                LOGGER.info("Ejecutando accion {}", plannedAction.asInvocation());
                ActionOutcome outcome = actionExecutor.execute(plannedAction, executionContext);
                executedActions.add(new ExecutionResult.ExecutedAction(plannedAction.asInvocation(), outcome.success()));
                state = stateUpdater.update(state, plannedAction, outcome);
                stateStore.save(state);

                if (!outcome.success()) {
                    LOGGER.info("La accion {} fallo, se relanza la planificacion", plannedAction.name());
                    replans++;
                    replanNeeded = true;
                    break;
                }
            }

            if (!replanNeeded) {
                return ExecutionResult.success("Ejecucion completada correctamente", executedActions, replans, state);
            }
        }
    }
}

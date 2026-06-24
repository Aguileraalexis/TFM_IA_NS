package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.ExecutionResult;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.port.ActionExecutor;
import com.tesis.nsframework.core.port.GoalMapper;
import com.tesis.nsframework.core.port.IntentInterpreter;
import com.tesis.nsframework.core.port.Planner;
import com.tesis.nsframework.core.port.PlanningProblemBuilder;
import com.tesis.nsframework.core.port.StateStore;
import com.tesis.nsframework.core.port.StateUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelExecutionOrchestratorTest {

    @Mock
    private IntentInterpreter intentInterpreter;
    @Mock
    private GoalMapper goalMapper;
    @Mock
    private PlanningProblemBuilder planningProblemBuilder;
    @Mock
    private Planner planner;
    @Mock
    private ActionExecutor actionExecutor;
    @Mock
    private StateUpdater stateUpdater;
    @Mock
    private StateStore stateStore;
    @Mock
    private TravelDomainPddlGenerator domainPddlGenerator;

    @Test
    void shouldFailFastWhenInterpreterOutputIsInvalid() {
        DomainMetadata metadata = new DomainMetadata(
                "travel",
                Set.of("plan_trip"),
                Map.of("plan_trip", "travel_goal")
        );

        when(intentInterpreter.interpret("input", metadata)).thenReturn(new InterpretationResult(
                "plan_trip",
                Map.of(
                        "originCityId", "MADR",
                        "targetCityIds", "LOGR"
                ),
                Map.of(),
                0.8,
                "{\"intent\":\"plan_trip\"}"
        ));

        TravelExecutionOrchestrator orchestrator = new TravelExecutionOrchestrator(
                intentInterpreter,
                goalMapper,
                planningProblemBuilder,
                planner,
                actionExecutor,
                stateUpdater,
                stateStore,
                metadata,
                domainPddlGenerator,
                PlannerOptions.defaults()
        );

        ExecutionResult result = orchestrator.run("input");

        assertFalse(result.success());
        assertTrue(result.message().contains("entities.travelDate es obligatorio"));
        verifyNoInteractions(goalMapper, planningProblemBuilder, planner, actionExecutor, stateUpdater, stateStore, domainPddlGenerator);
    }
}


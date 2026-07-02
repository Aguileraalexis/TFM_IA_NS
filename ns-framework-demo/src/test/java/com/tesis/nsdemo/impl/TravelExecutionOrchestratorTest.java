package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.ExecutionResult;
import com.tesis.nsframework.core.model.GoalSpec;
import com.tesis.nsframework.core.model.InterpretationResult;
import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.ActionExecutor;
import com.tesis.nsframework.core.port.GoalMapper;
import com.tesis.nsframework.core.port.IntentInterpreter;
import com.tesis.nsframework.core.port.Planner;
import com.tesis.nsframework.core.port.StateStore;
import com.tesis.nsframework.core.port.StateUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelExecutionOrchestratorTest {

    @Mock
    private IntentInterpreter intentInterpreter;
    @Mock
    private GoalMapper goalMapper;
    @Mock
    private TravelPlanningProblemBuilder planningProblemBuilder;
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

    @Test
    void shouldReplanWhenActionReturnsConflict409() {
        DomainMetadata metadata = travelMetadata();
        InterpretationResult interpretation = validInterpretation();
        GoalSpec goalSpec = new GoalSpec("travel_goal", Map.of(
                "travelerId", "1",
                "travelerSymbol", "traveler_1",
                "originCityId", "MADR",
                "targetCityIds", "LOGR",
                "requestedAttractionIds", "AT021",
                "travelDate", "2026-07-10"
        ), "travel", "viaje");

        PlanningProblem initialProblem = new PlanningProblem(
                "p1", "travel-dynamic", Set.of("traveler_1"), Set.of("(at traveler_1 MADR)"),
                "(visited-city LOGR)", Map.of("travelerId", "1", "travelerSymbol", "traveler_1", "travelDate", "2026-07-10")
        );
        PlanningProblem replannedProblem = new PlanningProblem(
                "p2", "travel-dynamic", Set.of("traveler_1"), Set.of("(at traveler_1 PARI)"),
                "(visited-city LOGR)", Map.of("travelerId", "1", "travelerSymbol", "traveler_1", "travelDate", "2026-07-10")
        );

        PlannedAction firstAction = new PlannedAction("book-flight", java.util.List.of("traveler_1", "madr", "pari"), Map.of());
        PlannedAction secondAction = new PlannedAction("book-flight", java.util.List.of("traveler_1", "pari", "logr"), Map.of());

        when(intentInterpreter.interpret("input", metadata)).thenReturn(interpretation);
        when(goalMapper.map(interpretation, metadata)).thenReturn(goalSpec);
        when(domainPddlGenerator.generate(any())).thenReturn("domain-1", "domain-2");
        when(planningProblemBuilder.build(any(SymbolicState.class), any(GoalSpec.class), any(TravelPlanningBlacklist.class)))
                .thenReturn(initialProblem, replannedProblem);
        when(planner.plan("domain-1", initialProblem, PlannerOptions.defaults()))
                .thenReturn(PlanResult.success(java.util.List.of(firstAction), "plan-1"));
        when(planner.plan("domain-2", replannedProblem, PlannerOptions.defaults()))
                .thenReturn(PlanResult.success(java.util.List.of(secondAction), "plan-2"));
        when(actionExecutor.execute(eq(firstAction), any())).thenReturn(ActionOutcome.failure(409, "Vuelo sin disponibilidad", Map.of()));
        when(actionExecutor.execute(eq(secondAction), any())).thenReturn(ActionOutcome.success(201, "ok", Map.of(), Map.of()));
        when(stateUpdater.update(any(SymbolicState.class), any(PlannedAction.class), any(ActionOutcome.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        assertTrue(result.success());
        assertEquals(1, result.replans());
        assertEquals(2, result.executedActions().size());
        assertEquals("book-flight(traveler_1, madr, pari)", result.executedActions().get(0).invocation());
        assertFalse(result.executedActions().get(0).success());
        assertEquals("book-flight(traveler_1, pari, logr)", result.executedActions().get(1).invocation());
        assertTrue(result.executedActions().get(1).success());
        verify(domainPddlGenerator, times(2)).generate(any());
        verify(planningProblemBuilder, times(2)).build(any(SymbolicState.class), any(GoalSpec.class), any(TravelPlanningBlacklist.class));
    }

    @Test
    void shouldAbortImmediatelyWhenFailureIsNotConflict409() {
        DomainMetadata metadata = travelMetadata();
        InterpretationResult interpretation = validInterpretation();
        GoalSpec goalSpec = new GoalSpec("travel_goal", Map.of(
                "travelerId", "1",
                "travelerSymbol", "traveler_1",
                "originCityId", "MADR",
                "targetCityIds", "LOGR",
                "requestedAttractionIds", "AT021",
                "travelDate", "2026-07-10"
        ), "travel", "viaje");
        PlanningProblem problem = new PlanningProblem(
                "p1", "travel-dynamic", Set.of("traveler_1"), Set.of("(at traveler_1 MADR)"),
                "(visited-city LOGR)", Map.of("travelerId", "1", "travelerSymbol", "traveler_1", "travelDate", "2026-07-10")
        );
        PlannedAction action = new PlannedAction("book-flight", java.util.List.of("traveler_1", "madr", "pari"), Map.of());

        when(intentInterpreter.interpret("input", metadata)).thenReturn(interpretation);
        when(goalMapper.map(interpretation, metadata)).thenReturn(goalSpec);
        when(domainPddlGenerator.generate(any())).thenReturn("domain-1");
        when(planningProblemBuilder.build(any(SymbolicState.class), any(GoalSpec.class), any(TravelPlanningBlacklist.class))).thenReturn(problem);
        when(planner.plan("domain-1", problem, PlannerOptions.defaults()))
                .thenReturn(PlanResult.success(java.util.List.of(action), "plan-1"));
        when(actionExecutor.execute(eq(action), any())).thenReturn(ActionOutcome.failure(500, "Fallo tecnico", Map.of()));
        when(stateUpdater.update(any(SymbolicState.class), any(PlannedAction.class), any(ActionOutcome.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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
        assertEquals(0, result.replans());
        assertEquals(1, result.executedActions().size());
        assertEquals("book-flight(traveler_1, madr, pari)", result.executedActions().getFirst().invocation());
        assertFalse(result.executedActions().getFirst().success());
        verify(domainPddlGenerator, times(1)).generate(any());
        verify(planningProblemBuilder, times(1)).build(any(SymbolicState.class), any(GoalSpec.class), any(TravelPlanningBlacklist.class));
    }

    @Test
    void shouldBlacklistFailedFlightRouteBeforeNextReplan() {
        DomainMetadata metadata = travelMetadata();
        InterpretationResult interpretation = validInterpretation();
        GoalSpec goalSpec = new GoalSpec("travel_goal", Map.of(
                "travelerId", "1",
                "travelerSymbol", "traveler_1",
                "originCityId", "MADR",
                "targetCityIds", "LOGR",
                "requestedAttractionIds", "AT021",
                "travelDate", "2026-07-10"
        ), "travel", "viaje");

        PlanningProblem initialProblem = new PlanningProblem(
                "p1", "travel-dynamic", Set.of("traveler_1"), Set.of("(at traveler_1 MADR)"),
                "(visited-city LOGR)", Map.of("travelerId", "1", "travelerSymbol", "traveler_1", "travelDate", "2026-07-10")
        );
        PlanningProblem replannedProblem = new PlanningProblem(
                "p2", "travel-dynamic", Set.of("traveler_1"), Set.of("(at traveler_1 MADR)"),
                "(visited-city LOGR)", Map.of("travelerId", "1", "travelerSymbol", "traveler_1", "travelDate", "2026-07-10")
        );

        PlannedAction failedAction = new PlannedAction("book-flight", java.util.List.of("traveler_1", "madr", "pari"), Map.of());
        PlannedAction successAction = new PlannedAction("book-flight", java.util.List.of("traveler_1", "madr", "logr"), Map.of());

        when(intentInterpreter.interpret("input", metadata)).thenReturn(interpretation);
        when(goalMapper.map(interpretation, metadata)).thenReturn(goalSpec);
        when(domainPddlGenerator.generate(any())).thenReturn("domain-1", "domain-2");
        when(planningProblemBuilder.build(any(SymbolicState.class), any(GoalSpec.class), any(TravelPlanningBlacklist.class)))
                .thenReturn(initialProblem, replannedProblem);
        when(planner.plan("domain-1", initialProblem, PlannerOptions.defaults()))
                .thenReturn(PlanResult.success(java.util.List.of(failedAction), "plan-1"));
        when(planner.plan("domain-2", replannedProblem, PlannerOptions.defaults()))
                .thenReturn(PlanResult.success(java.util.List.of(successAction), "plan-2"));
        when(actionExecutor.execute(eq(failedAction), any())).thenReturn(ActionOutcome.failure(409, "Vuelo sin disponibilidad", Map.of()));
        when(actionExecutor.execute(eq(successAction), any())).thenReturn(ActionOutcome.success(201, "ok", Map.of(), Map.of()));
        when(stateUpdater.update(any(SymbolicState.class), any(PlannedAction.class), any(ActionOutcome.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        assertTrue(result.success());
        assertEquals(2, result.executedActions().size());
        assertFalse(result.executedActions().get(0).success());
        assertTrue(result.executedActions().get(1).success());
        org.mockito.ArgumentCaptor<TravelPlanningBlacklist> blacklistCaptor = org.mockito.ArgumentCaptor.forClass(TravelPlanningBlacklist.class);
        verify(planningProblemBuilder, times(2)).build(any(SymbolicState.class), any(GoalSpec.class), blacklistCaptor.capture());
        assertTrue(blacklistCaptor.getAllValues().get(1).isFlightRouteBlacklisted("MADR", "PARI"));
    }

    @Test
    void shouldBlacklistFailedHotelBeforeNextReplan() {
        DomainMetadata metadata = travelMetadata();
        InterpretationResult interpretation = validInterpretation();
        GoalSpec goalSpec = new GoalSpec("travel_goal", Map.of(
                "travelerId", "1",
                "travelerSymbol", "traveler_1",
                "originCityId", "MADR",
                "targetCityIds", "LOGR",
                "requestedAttractionIds", "AT021",
                "travelDate", "2026-07-10"
        ), "travel", "viaje");

        PlanningProblem initialProblem = new PlanningProblem(
                "p1", "travel-dynamic", Set.of("traveler_1"), Set.of("(at traveler_1 LOGR)"),
                "(hotel-booked traveler_1 HT020 LOGR)", Map.of("travelerId", "1", "travelerSymbol", "traveler_1", "travelDate", "2026-07-10")
        );
        PlanningProblem replannedProblem = new PlanningProblem(
                "p2", "travel-dynamic", Set.of("traveler_1"), Set.of("(at traveler_1 LOGR)"),
                "(hotel-booked traveler_1 HT021 LOGR)", Map.of("travelerId", "1", "travelerSymbol", "traveler_1", "travelDate", "2026-07-10")
        );

        PlannedAction failedAction = new PlannedAction("book-hotel", java.util.List.of("traveler_1", "ht020", "logr"), Map.of());
        PlannedAction successAction = new PlannedAction("book-hotel", java.util.List.of("traveler_1", "ht021", "logr"), Map.of());

        when(intentInterpreter.interpret("input", metadata)).thenReturn(interpretation);
        when(goalMapper.map(interpretation, metadata)).thenReturn(goalSpec);
        when(domainPddlGenerator.generate(any())).thenReturn("domain-1", "domain-2");
        when(planningProblemBuilder.build(any(SymbolicState.class), any(GoalSpec.class), any(TravelPlanningBlacklist.class)))
                .thenReturn(initialProblem, replannedProblem);
        when(planner.plan("domain-1", initialProblem, PlannerOptions.defaults()))
                .thenReturn(PlanResult.success(java.util.List.of(failedAction), "plan-1"));
        when(planner.plan("domain-2", replannedProblem, PlannerOptions.defaults()))
                .thenReturn(PlanResult.success(java.util.List.of(successAction), "plan-2"));
        when(actionExecutor.execute(eq(failedAction), any())).thenReturn(ActionOutcome.failure(409, "Hotel sin disponibilidad", Map.of()));
        when(actionExecutor.execute(eq(successAction), any())).thenReturn(ActionOutcome.success(201, "ok", Map.of(), Map.of()));
        when(stateUpdater.update(any(SymbolicState.class), any(PlannedAction.class), any(ActionOutcome.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        assertTrue(result.success());
        assertEquals(2, result.executedActions().size());
        assertFalse(result.executedActions().get(0).success());
        assertTrue(result.executedActions().get(1).success());
        org.mockito.ArgumentCaptor<TravelPlanningBlacklist> blacklistCaptor = org.mockito.ArgumentCaptor.forClass(TravelPlanningBlacklist.class);
        verify(planningProblemBuilder, times(2)).build(any(SymbolicState.class), any(GoalSpec.class), blacklistCaptor.capture());
        assertTrue(blacklistCaptor.getAllValues().get(1).isHotelBlacklisted("HT020"));
    }

    private DomainMetadata travelMetadata() {
        return new DomainMetadata(
                "travel",
                Set.of("plan_trip"),
                Map.of("plan_trip", "travel_goal")
        );
    }

    private InterpretationResult validInterpretation() {
        return new InterpretationResult(
                "plan_trip",
                Map.of(
                        "originCityId", "MADR",
                        "targetCityIds", "LOGR",
                        "travelDate", "2026-07-10",
                        "travelerId", "1",
                        "travelerSymbol", "traveler_1"
                ),
                Map.of(),
                0.8,
                "{\"intent\":\"plan_trip\"}"
        );
    }
}


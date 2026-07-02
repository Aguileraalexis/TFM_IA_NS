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
import com.tesis.nsframework.core.port.StateStore;
import com.tesis.nsframework.core.port.StateUpdater;
import com.tesis.nsframework.core.model.DomainMetadata;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class TravelExecutionOrchestrator implements ExecutionOrchestrator {

    private final IntentInterpreter intentInterpreter;
    private final GoalMapper goalMapper;
    private final TravelPlanningProblemBuilder planningProblemBuilder;
    private final Planner planner;
    private final ActionExecutor actionExecutor;
    private final StateUpdater stateUpdater;
    private final StateStore stateStore;
    private final DomainMetadata domainMetadata;
    private final TravelDomainPddlGenerator domainPddlGenerator;
    private final PlannerOptions plannerOptions;

    public TravelExecutionOrchestrator(IntentInterpreter intentInterpreter,
                                       GoalMapper goalMapper,
                                       TravelPlanningProblemBuilder planningProblemBuilder,
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
        String validationError = validateInterpretation(interpretation);
        if (validationError != null) {
            log.warn("Respuesta invalida del interpreter. error={}, rawResponse={}",
                    validationError,
                    interpretation == null ? null : interpretation.rawResponse());
            return ExecutionResult.failure("Respuesta invalida del interpreter: " + validationError,
                    List.of(),
                    0,
                    new SymbolicState());
        }

        GoalSpec goalSpec = goalMapper.map(interpretation, domainMetadata);
        SymbolicState state = new SymbolicState();
        stateStore.save(state);
        List<ExecutionResult.ExecutedAction> executedActions = new ArrayList<>();
        int replans = 0;
        TravelPlanningBlacklist blacklist = new TravelPlanningBlacklist();

        java.time.LocalDate travelDate = extractTravelDate(interpretation);
        while (true) {
            ExecutionContext executionContext = buildExecutionContext(interpretation);
            String domainPddl = domainPddlGenerator.generate(travelDate);
            PlanningProblem problem = planningProblemBuilder.build(state, goalSpec, blacklist);
            // Enriquecer el contexto de ejecucion con metadatos de planificacion (rutas, hoteles, fechas)
            // para que el ejecutor de acciones resuelva parametros sin depender de metadatos por accion
            // (DockerPlanner no los completa: solo parsea nombre y argumentos de la accion).
            problem.metadata().forEach(executionContext::put);
            PlanResult planResult = planner.plan(domainPddl, problem, plannerOptions);
            if (!planResult.success() || planResult.actions().isEmpty()) {
                log.warn("No se encontro un plan de viaje valido: {}", planResult.message());
                return ExecutionResult.failure(planResult.message(), executedActions, replans, state);
            }

            boolean replanNeeded = false;
            for (PlannedAction plannedAction : planResult.actions()) {
                log.info("Ejecutando accion de viaje {}", plannedAction.asInvocation());
                ActionOutcome outcome = actionExecutor.execute(plannedAction, executionContext);
                executedActions.add(new ExecutionResult.ExecutedAction(plannedAction.asInvocation(), outcome.success()));
                state = stateUpdater.update(state, plannedAction, outcome);
                stateStore.save(state);

                if (!outcome.success()) {
                    if (shouldBlacklistForReplan(outcome.statusCode(), plannedAction)) {
                        rememberFailedElement(blacklist, plannedAction);
                        log.warn("La accion {} devolvio conflicto 409; se relanza la planificacion con catalogo actualizado",
                                plannedAction.asInvocation());
                        replans++;
                        replanNeeded = true;
                        break;
                    }
                    return ExecutionResult.failure(outcome.message(), executedActions, replans, state);
                }
            }

            if (!replanNeeded) {
                return ExecutionResult.success("Viaje completado exitosamente", executedActions, replans, state);
            }
        }
    }

    private ExecutionContext buildExecutionContext(InterpretationResult interpretation) {
        ExecutionContext executionContext = new ExecutionContext(interpretation.entities());
        interpretation.constraints().forEach(executionContext::put);
        return executionContext;
    }

    private String validateInterpretation(InterpretationResult interpretation) {
        if (interpretation == null) {
            return "faltan datos de interpretacion";
        }
        if (interpretation.intent() == null || interpretation.intent().isBlank()) {
            return "intent es obligatorio";
        }
        if (!domainMetadata.supportsIntent(interpretation.intent())) {
            return "intent no soportado: " + interpretation.intent();
        }

        Map<String, Object> entities = interpretation.entities();
        String originCityId = requiredEntityAsText(entities, "originCityId");
        if (originCityId == null) {
            return "entities.originCityId es obligatorio";
        }

        String targetCityIds = requiredEntityAsText(entities, "targetCityIds");
        if (targetCityIds == null || !hasPipeSeparatedValues(targetCityIds)) {
            return "entities.targetCityIds debe incluir al menos una ciudad";
        }

        String travelDate = requiredEntityAsText(entities, "travelDate");
        if (travelDate == null) {
            return "entities.travelDate es obligatorio";
        }
        try {
            LocalDate.parse(travelDate);
        } catch (Exception ex) {
            return "entities.travelDate debe tener formato YYYY-MM-DD";
        }

        if (interpretation.confidence() != null
                && (interpretation.confidence() < 0.0 || interpretation.confidence() > 1.0)) {
            return "confidence debe estar en el rango [0, 1]";
        }

        return null;
    }

    private String requiredEntityAsText(Map<String, Object> entities, String key) {
        Object value = entities.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private boolean hasPipeSeparatedValues(String raw) {
        return Stream.of(raw.split("\\|"))
                .map(String::trim)
                .anyMatch(value -> !value.isBlank());
    }

    private java.time.LocalDate extractTravelDate(InterpretationResult interpretation) {
        Object dateObj = interpretation.entities().get("travelDate");
        if (dateObj instanceof String dateStr) {
            try {
                return java.time.LocalDate.parse(dateStr);
            } catch (Exception ex) {
                log.warn("No se pudo parsear la fecha de viaje: {}", dateStr, ex);
                return null;
            }
        } else if (dateObj instanceof java.time.LocalDate date) {
            return date;
        }
        return null;
    }

    private boolean shouldBlacklistForReplan(int statusCode, PlannedAction action) {
        return statusCode == 409 && ("book-flight".equals(action.name()) || "book-hotel".equals(action.name()));
    }

    private void rememberFailedElement(TravelPlanningBlacklist blacklist, PlannedAction action) {
        List<String> arguments = action.arguments();
        if ("book-flight".equals(action.name()) && arguments.size() >= 3) {
            blacklist.blacklistFlightRoute(arguments.get(1), arguments.get(2));
            return;
        }
        if ("book-hotel".equals(action.name()) && arguments.size() >= 2) {
            blacklist.blacklistHotel(arguments.get(1).toUpperCase(Locale.ROOT));
        }
    }
}

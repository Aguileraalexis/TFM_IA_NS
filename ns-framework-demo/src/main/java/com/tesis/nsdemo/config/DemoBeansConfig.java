package com.tesis.nsdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsdemo.impl.*;
import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.*;
import com.tesis.nsframework.core.service.DefaultGoalMapper;
import com.tesis.nsframework.core.service.DefaultStateUpdater;
import com.tesis.nsframework.core.service.InMemoryStateStore;
import com.tesis.nsframework.llm.http.HttpLlmPlanner;
import com.tesis.nsframework.planner.docker.DockerPlanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Configuration
public class DemoBeansConfig {

    @Bean
    public GoalMapper goalMapper() {
        return new DefaultGoalMapper();
    }

    @Bean
    public StateUpdater stateUpdater() {
        return new DefaultStateUpdater();
    }

    @Bean
    public StateStore stateStore() {
        return new InMemoryStateStore(new SymbolicState());
    }

    @Bean
    public DomainMetadata domainMetadata() {
        return new DomainMetadata(
                TravelDomainPddlGenerator.DOMAIN_NAME,
                Set.of("plan_trip"),
                Map.of(
                        "plan_trip", "travel_goal"
                )
        );
    }

    @Bean
    public PlannerOptions plannerOptions(TravelDemoProperties properties) {
        TravelDemoProperties.PlannerProperties plannerProps = properties.getPlanner();
        return new PlannerOptions(Duration.ofSeconds(plannerProps.getTimeoutSeconds()), "astar(lmcut())");
    }

    @Bean
    public Planner planner(TravelDemoProperties properties, ObjectMapper objectMapper) {
        TravelDemoProperties.PlannerProperties plannerProps = properties.getPlanner();
        String plannerType = plannerProps.getType() == null ? "docker" : plannerProps.getType().trim().toLowerCase();
        return switch (plannerType) {
            case "docker" -> new DockerPlanner(
                    plannerProps.getDockerImage(),
                    plannerProps.getContainerCommand()
            );
            case "demo", "java" -> new TravelDemoPlanner();
            case "http-llm" -> createHttpLlmPlanner(objectMapper, plannerProps.getHttpLlm());
            default -> throw new IllegalStateException("Tipo de planner no soportado: " + plannerProps.getType());
        };
    }

    private Planner createHttpLlmPlanner(ObjectMapper objectMapper,
                                         TravelDemoProperties.HttpLlmProperties llmProperties) {
        if (llmProperties == null || !StringUtils.hasText(llmProperties.getEndpoint())) {
            throw new IllegalStateException("demo.travel.planner.http-llm.endpoint debe configurarse cuando planner.type=http-llm");
        }

        Duration timeout = llmProperties.getTimeout() == null ? Duration.ofSeconds(60) : llmProperties.getTimeout();
        return new HttpLlmPlanner(
                objectMapper,
                URI.create(llmProperties.getEndpoint()),
                llmProperties.getApiKey(),
                timeout,
                sanitizeRequestOptions(llmProperties.getRequest())
        );
    }

    private Map<String, Object> sanitizeRequestOptions(Map<String, Object> requestOptions) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (requestOptions == null) {
            return sanitized;
        }
        requestOptions.forEach((key, value) -> {
            if (value instanceof String stringValue && !StringUtils.hasText(stringValue)) {
                return;
            }
            if (value != null) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    @Bean
    public ExecutionOrchestrator executionOrchestrator(IntentInterpreter intentInterpreter,
                                                       GoalMapper goalMapper,
                                                       TravelPlanningProblemBuilder planningProblemBuilder,
                                                       Planner planner,
                                                       ActionExecutor actionExecutor,
                                                       StateUpdater stateUpdater,
                                                       StateStore stateStore,
                                                       DomainMetadata domainMetadata,
                                                       TravelDomainPddlGenerator domainPddlGenerator,
                                                       PlannerOptions plannerOptions) {
        return new TravelExecutionOrchestrator(
                intentInterpreter,
                goalMapper,
                planningProblemBuilder,
                planner,
                actionExecutor,
                stateUpdater,
                stateStore,
                domainMetadata,
                domainPddlGenerator,
                plannerOptions
        );
    }
}

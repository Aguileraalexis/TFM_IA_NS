package com.tesis.nsdemo.config;

import com.tesis.nsdemo.impl.TravelExecutionOrchestrator;
import com.tesis.nsdemo.travel.TravelDomainPddlGenerator;
import com.tesis.nsframework.core.model.DomainMetadata;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.SymbolicState;
import com.tesis.nsframework.core.port.*;
import com.tesis.nsframework.core.service.DefaultGoalMapper;
import com.tesis.nsframework.core.service.DefaultStateUpdater;
import com.tesis.nsframework.core.service.InMemoryStateStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public PlannerOptions plannerOptions() {
        return PlannerOptions.defaults();
    }

    @Bean
    public ExecutionOrchestrator executionOrchestrator(IntentInterpreter intentInterpreter,
                                                       GoalMapper goalMapper,
                                                       PlanningProblemBuilder planningProblemBuilder,
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

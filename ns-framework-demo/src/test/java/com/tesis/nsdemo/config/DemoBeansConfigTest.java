package com.tesis.nsdemo.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsdemo.impl.TravelDemoPlanner;
import com.tesis.nsframework.core.port.Planner;
import com.tesis.nsframework.llm.http.HttpLlmPlanner;
import com.tesis.nsframework.planner.docker.DockerPlanner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
class DemoBeansConfigTest {
    private final DemoBeansConfig config = new DemoBeansConfig();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Test
    void shouldCreateDockerPlannerByDefault() {
        Planner planner = config.planner(new TravelDemoProperties(), objectMapper);
        assertInstanceOf(DockerPlanner.class, planner);
    }
    @Test
    void shouldCreateJavaBaselinePlannerWhenConfigured() {
        TravelDemoProperties properties = new TravelDemoProperties();
        properties.getPlanner().setType("demo");
        Planner planner = config.planner(properties, objectMapper);
        assertInstanceOf(TravelDemoPlanner.class, planner);
    }
    @Test
    void shouldCreateHttpLlmPlannerWhenConfigured() {
        TravelDemoProperties properties = new TravelDemoProperties();
        properties.getPlanner().setType("http-llm");
        properties.getPlanner().getHttpLlm().setEndpoint("http://localhost:11434/api/generate");
        Planner planner = config.planner(properties, objectMapper);
        assertInstanceOf(HttpLlmPlanner.class, planner);
    }
    @Test
    void shouldFailFastWhenHttpLlmPlannerHasNoEndpoint() {
        TravelDemoProperties properties = new TravelDemoProperties();
        properties.getPlanner().setType("http-llm");
        assertThrows(IllegalStateException.class, () -> config.planner(properties, objectMapper));
    }
}
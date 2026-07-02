package com.tesis.nsdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "demo.travel")
public class TravelDemoProperties {
    private String defaultTravelerId = "1";
    private int defaultTravelDateOffsetDays = 30;

    @NestedConfigurationProperty
    private InterpreterProperties interpreter = new InterpreterProperties();

    @NestedConfigurationProperty
    private PlannerProperties planner = new PlannerProperties();

    public String getDefaultTravelerId() {
        return defaultTravelerId;
    }

    public void setDefaultTravelerId(String defaultTravelerId) {
        this.defaultTravelerId = defaultTravelerId;
    }

    public int getDefaultTravelDateOffsetDays() {
        return defaultTravelDateOffsetDays;
    }

    public void setDefaultTravelDateOffsetDays(int defaultTravelDateOffsetDays) {
        this.defaultTravelDateOffsetDays = defaultTravelDateOffsetDays;
    }

    public InterpreterProperties getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(InterpreterProperties interpreter) {
        this.interpreter = interpreter;
    }

    public PlannerProperties getPlanner() {
        return planner;
    }

    public void setPlanner(PlannerProperties planner) {
        this.planner = planner;
    }

    public static class InterpreterProperties {
        private String type = "rule-based";

        @NestedConfigurationProperty
        private HttpLlmProperties httpLlm = new HttpLlmProperties();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public HttpLlmProperties getHttpLlm() {
            return httpLlm;
        }

        public void setHttpLlm(HttpLlmProperties httpLlm) {
            this.httpLlm = httpLlm;
        }
    }

    public static class HttpLlmProperties {
        private String endpoint;
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(30);
        private Map<String, Object> request = new LinkedHashMap<>();

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Map<String, Object> getRequest() {
            return request;
        }

        public void setRequest(Map<String, Object> request) {
            this.request = request;
        }
    }

    public static class PlannerProperties {
        private String type = "docker";
        private String dockerImage = "aibasel/downward";
        private String containerCommand = "--plan-file /planner/plan.txt /planner/domain.pddl /planner/problem.pddl --search 'astar(lmcut())'";
        private boolean planWrittenToFile = true;
        private int timeoutSeconds = 120;

        @NestedConfigurationProperty
        private HttpLlmProperties httpLlm = new HttpLlmProperties();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDockerImage() {
            return dockerImage;
        }

        public void setDockerImage(String dockerImage) {
            this.dockerImage = dockerImage;
        }

        public String getContainerCommand() {
            return containerCommand;
        }

        public void setContainerCommand(String containerCommand) {
            this.containerCommand = containerCommand;
        }

        public boolean isPlanWrittenToFile() {
            return planWrittenToFile;
        }

        public void setPlanWrittenToFile(boolean planWrittenToFile) {
            this.planWrittenToFile = planWrittenToFile;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public HttpLlmProperties getHttpLlm() {
            return httpLlm;
        }

        public void setHttpLlm(HttpLlmProperties httpLlm) {
            this.httpLlm = httpLlm;
        }
    }
}


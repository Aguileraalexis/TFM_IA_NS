package com.tesis.nsdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "demo.travel")
public class TravelDemoProperties {
    private String defaultTravelerId = "1";
    private int defaultTravelDateOffsetDays = 30;

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

    public PlannerProperties getPlanner() {
        return planner;
    }

    public void setPlanner(PlannerProperties planner) {
        this.planner = planner;
    }

    public static class PlannerProperties {
        private String dockerImage = "aibasel/downward";
        private String containerCommand = "fast-downward.py --plan-file /planner/plan.txt /planner/domain.pddl /planner/problem.pddl --search 'astar(lmcut())'";
        private boolean planWrittenToFile = true;
        private int timeoutSeconds = 120;

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
    }
}


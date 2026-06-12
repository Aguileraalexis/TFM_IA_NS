package com.tesis.nsdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.travel")
public class TravelDemoProperties {
    private String defaultTravelerId = "1";
    private int defaultTravelDateOffsetDays = 30;

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
}


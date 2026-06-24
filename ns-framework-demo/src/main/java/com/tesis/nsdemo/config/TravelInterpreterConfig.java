package com.tesis.nsdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsdemo.impl.TravelIntentInterpreter;
import com.tesis.nsdemo.travel.TravelCatalogService;
import com.tesis.nsframework.core.port.IntentInterpreter;
import com.tesis.nsframework.llm.http.HttpLlmIntentInterpreter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class TravelInterpreterConfig {

    @Bean
    @ConditionalOnProperty(prefix = "demo.travel.interpreter", name = "type", havingValue = "rule-based", matchIfMissing = true)
    public IntentInterpreter travelIntentInterpreter(TravelCatalogService travelCatalogService,
                                                     TravelDemoProperties properties) {
        return new TravelIntentInterpreter(travelCatalogService, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "demo.travel.interpreter", name = "type", havingValue = "http-llm")
    public IntentInterpreter httpLlmIntentInterpreter(ObjectMapper objectMapper,
                                                      TravelDemoProperties properties) {
        TravelDemoProperties.HttpLlmProperties llmProperties = properties.getInterpreter().getHttpLlm();
        if (!StringUtils.hasText(llmProperties.getEndpoint())) {
            throw new IllegalStateException("demo.travel.interpreter.http-llm.endpoint must be configured when type=http-llm");
        }

        Duration timeout = llmProperties.getTimeout() == null ? Duration.ofSeconds(30) : llmProperties.getTimeout();
        return new HttpLlmIntentInterpreter(
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
}


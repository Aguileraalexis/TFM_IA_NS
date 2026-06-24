package com.tesis.nsdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.nsdemo.impl.TravelIntentInterpreter;
import com.tesis.nsdemo.travel.TravelCatalogService;
import com.tesis.nsframework.core.port.IntentInterpreter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TravelInterpreterConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class, TravelInterpreterConfig.class);

    @Test
    void shouldUseRuleBasedInterpreterByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IntentInterpreter.class);
            assertThat(context.getBean(IntentInterpreter.class)).isInstanceOf(TravelIntentInterpreter.class);
        });
    }

    @Test
    void shouldUseHttpLlmInterpreterWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "demo.travel.interpreter.type=http-llm",
                        "demo.travel.interpreter.http-llm.endpoint=http://localhost:11434/api/generate",
                        "demo.travel.interpreter.http-llm.request.model=llama3.1",
                        "demo.travel.interpreter.http-llm.request.stream=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(IntentInterpreter.class);
                    assertThat(context.getBean(IntentInterpreter.class).getClass().getName())
                            .isEqualTo("com.tesis.nsframework.llm.http.HttpLlmIntentInterpreter");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TravelDemoProperties.class)
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        TravelCatalogService travelCatalogService() {
            return new TravelCatalogService(null, null, null);
        }
    }
}



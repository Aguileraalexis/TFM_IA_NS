package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.config.TravelDemoProperties;
import com.tesis.nsdemo.travel.TravelCatalogService;
import com.tesis.nsdemo.travel.TravelCatalogSnapshot;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.InterpretationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TravelIntentInterpreterTest {

    @Test
    void shouldInferOriginAndDestinationFromSamplePrompt() {
        TravelIntentInterpreter interpreter = new TravelIntentInterpreter(catalogService(), demoProperties());

        InterpretationResult result = interpreter.interpret(
                "Quiero viajar desde Madrid para visitar Universidad de La Rioja el 2026-07-10",
                null);

        assertEquals("MADR", result.entities().get("originCityId"));
        assertEquals("LOGR", result.entities().get("targetCityIds"));
        assertEquals("AT021", result.entities().get("requestedAttractionIds"));
        assertEquals("2026-07-10", result.entities().get("travelDate"));
    }

    @Test
    void shouldThrowErrorWhenOriginCityCannotBeInferredFromPrompt() {
        TravelIntentInterpreter interpreter = new TravelIntentInterpreter(catalogService(), demoProperties());

        assertThrows(FrameworkException.class, () -> interpreter.interpret(
                "Quiero visitar Universidad de La Rioja el 2026-07-10",
                null));
    }

    private static TravelCatalogService catalogService() {
        return new TravelCatalogService(null, null, null) {
            @Override
            public TravelCatalogSnapshot fetchSnapshot() {
                return new TravelCatalogSnapshot(
                        List.of(
                                new CityDto("MADR", "Madrid", "ESPA"),
                                new CityDto("LOGR", "Logroño", "ESPA")
                        ),
                        List.of(new AttractionDto("AT021", "LOGR", "Universidad de La Rioja", "Universidad")),
                        List.of(),
                        List.of(),
                        java.util.Map.of(),
                        java.util.Map.of()
                );
            }
        };
    }

    private static TravelDemoProperties demoProperties() {
        TravelDemoProperties properties = new TravelDemoProperties();
        properties.setDefaultTravelerId("1");
        properties.setDefaultTravelDateOffsetDays(30);
        return properties;
    }
}


package com.tesis.nsdemo.travel;

import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.HotelDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelDomainPddlGeneratorTest {

    @Test
    void shouldGenerateDynamicDomainWithCatalogConstants() {
        TravelCatalogService catalogService = new TravelCatalogService(null, null, null) {
            @Override
            public TravelCatalogSnapshot fetchSnapshot(java.time.LocalDate travelDate) {
                return new TravelCatalogSnapshot(
                        List.of(new CityDto("MADR", "Madrid", "ESPA"), new CityDto("PARI", "Paris", "FRAN")),
                        List.of(new AttractionDto("AT017", "PARI", "Torre Eiffel", "Monumento")),
                        List.of(new FlightDto("V001", "MADR", "PARI", "AL01")),
                        List.of(new HotelDto("HT018", "Melia Madrid Centro", "MADR"), new HotelDto("HT016", "Hilton Paris Opera", "PARI")),
                        Map.of("MADR", List.of(new HotelDto("HT018", "Melia Madrid Centro", "MADR")), "PARI", List.of(new HotelDto("HT016", "Hilton Paris Opera", "PARI"))),
                        Map.of("PARI", List.of(new AttractionDto("AT017", "PARI", "Torre Eiffel", "Monumento")))
                );
            }
        };

        String domain = new TravelDomainPddlGenerator(catalogService).generate();

        assertTrue(domain.contains("(define (domain travel-dynamic)"));
        assertTrue(domain.contains("MADR PARI - city"));
        assertTrue(domain.contains("HT018 HT016 - hotel") || domain.contains("HT016 HT018 - hotel"));
        assertTrue(domain.contains("AT017 - attraction"));
        assertTrue(domain.contains("(:action book-flight"));
        assertTrue(domain.contains(":parameters (?t - traveler ?from - city ?to - city)"));
        assertTrue(domain.contains("(:action book-hotel"));
        assertTrue(domain.contains("(:action visit-attraction"));
    }
}



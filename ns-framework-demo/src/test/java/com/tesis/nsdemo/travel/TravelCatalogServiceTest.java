package com.tesis.nsdemo.travel;

import com.tesis.nsdemo.client.FlightServiceClient;
import com.tesis.nsdemo.client.HotelServiceClient;
import com.tesis.nsdemo.client.TourismServiceClient;
import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.FlightReservationDto;
import com.tesis.nsdemo.client.dto.FlightReservationRequest;
import com.tesis.nsdemo.client.dto.HotelDto;
import com.tesis.nsdemo.client.dto.HotelReservationDto;
import com.tesis.nsdemo.client.dto.HotelReservationRequest;
import com.tesis.nsframework.core.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelCatalogServiceTest {

    @Test
    void shouldWrapFlightCatalogFailureAsExternalServiceException() {
        FlightServiceClient flightClient = new FlightServiceClient() {
            @Override
            public List<CityDto> getCities() {
                return List.of();
            }

            @Override
            public List<FlightDto> getFlights(LocalDate fecha) {
                throw new IllegalStateException("servicio caido");
            }

            @Override
            public FlightReservationDto createReservation(FlightReservationRequest request) {
                return null;
            }
        };
        HotelServiceClient hotelClient = new HotelServiceClient() {
            @Override
            public List<CityDto> getCities() {
                return List.of();
            }

            @Override
            public List<HotelDto> getHotels(String ciudadId, LocalDate fecha) {
                return List.of();
            }

            @Override
            public HotelReservationDto createReservation(HotelReservationRequest req) {
                return null;
            }
        };
        TourismServiceClient tourismClient = new TourismServiceClient() {
            @Override
            public List<CityDto> getCities() {
                return List.of(new CityDto("MADR", "Madrid", "ES"));
            }

            @Override
            public List<AttractionDto> getAttractions() {
                return List.of();
            }
        };

        TravelCatalogService service = new TravelCatalogService(flightClient, hotelClient, tourismClient);

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> service.fetchSnapshot(LocalDate.of(2026, 7, 10)));

        assertEquals(503, ex.suggestedHttpStatus());
        assertTrue(ex.getMessage().contains("servicio mock de vuelos"));
    }
}

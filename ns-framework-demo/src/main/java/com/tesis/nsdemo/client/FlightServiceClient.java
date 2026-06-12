package com.tesis.nsdemo.client;

import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.FlightReservationDto;
import com.tesis.nsdemo.client.dto.FlightReservationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "flightServiceClient", url = "${demo.travel.services.flight.url}")
public interface FlightServiceClient {

    @GetMapping("/ciudades")
    List<CityDto> getCities();

    @GetMapping("/vuelos")
    List<FlightDto> getFlights();

    @PostMapping("/reservas-vuelos")
    FlightReservationDto createReservation(@RequestBody FlightReservationRequest request);
}


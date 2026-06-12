package com.tesis.nsdemo.client;

import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.HotelDto;
import com.tesis.nsdemo.client.dto.HotelReservationDto;
import com.tesis.nsdemo.client.dto.HotelReservationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "hotelServiceClient", url = "${demo.travel.services.hotel.url}")
public interface HotelServiceClient {

    @GetMapping("/ciudades")
    List<CityDto> getCities();

    @GetMapping("/hoteles")
    List<HotelDto> getHotels(@RequestParam(name = "ciudadId", required = false) String ciudadId);

    @PostMapping("/reservas")
    HotelReservationDto createReservation(@RequestBody HotelReservationRequest request);
}


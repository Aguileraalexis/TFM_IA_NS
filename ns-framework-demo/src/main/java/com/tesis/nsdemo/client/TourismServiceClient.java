package com.tesis.nsdemo.client;

import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "tourismServiceClient", url = "${demo.travel.services.tourism.url}")
public interface TourismServiceClient {

    @GetMapping("/ciudades")
    List<CityDto> getCities();

    @GetMapping("/atractivos")
    List<AttractionDto> getAttractions();
}


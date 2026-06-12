package com.tesis.nsdemo.travel;

import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.HotelDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record TravelCatalogSnapshot(
        List<CityDto> cities,
        List<AttractionDto> attractions,
        List<FlightDto> flights,
        List<HotelDto> hotels,
        Map<String, List<HotelDto>> hotelsByCity,
        Map<String, List<AttractionDto>> attractionsByCity
) {
    public TravelCatalogSnapshot {
        cities = cities == null ? List.of() : List.copyOf(cities);
        attractions = attractions == null ? List.of() : List.copyOf(attractions);
        flights = flights == null ? List.of() : List.copyOf(flights);
        hotels = hotels == null ? List.of() : List.copyOf(hotels);
        hotelsByCity = hotelsByCity == null ? Collections.emptyMap() : Collections.unmodifiableMap(hotelsByCity);
        attractionsByCity = attractionsByCity == null ? Collections.emptyMap() : Collections.unmodifiableMap(attractionsByCity);
    }
}


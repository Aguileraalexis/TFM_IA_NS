package com.tesis.nsdemo.travel;

import com.tesis.nsdemo.client.FlightServiceClient;
import com.tesis.nsdemo.client.HotelServiceClient;
import com.tesis.nsdemo.client.TourismServiceClient;
import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.HotelDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TravelCatalogService {
    private final FlightServiceClient flightServiceClient;
    private final HotelServiceClient hotelServiceClient;
    private final TourismServiceClient tourismServiceClient;

    public TravelCatalogService(FlightServiceClient flightServiceClient,
                                HotelServiceClient hotelServiceClient,
                                TourismServiceClient tourismServiceClient) {
        this.flightServiceClient = flightServiceClient;
        this.hotelServiceClient = hotelServiceClient;
        this.tourismServiceClient = tourismServiceClient;
    }

    public TravelCatalogSnapshot fetchSnapshot() {
        return fetchSnapshot(null);
    }

    public TravelCatalogSnapshot fetchSnapshot(java.time.LocalDate travelDate) {
        List<CityDto> cities = tourismServiceClient.getCities().stream()
                .sorted(Comparator.comparing(CityDto::id))
                .toList();
        List<AttractionDto> attractions = tourismServiceClient.getAttractions().stream()
                .sorted(Comparator.comparing(AttractionDto::id))
                .toList();
        List<FlightDto> flights = flightServiceClient.getFlights(travelDate).stream()
                .sorted(Comparator.comparing(FlightDto::id))
                .toList();
        List<HotelDto> hotels = cities.stream()
                .flatMap(city -> hotelServiceClient.getHotels(city.id(), travelDate).stream())
                .sorted(Comparator.comparing(HotelDto::id))
                .toList();

        Map<String, List<HotelDto>> hotelsByCity = hotels.stream()
                .collect(Collectors.groupingBy(HotelDto::ciudadId, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<AttractionDto>> attractionsByCity = attractions.stream()
                .collect(Collectors.groupingBy(AttractionDto::ciudadId, LinkedHashMap::new, Collectors.toList()));

        return new TravelCatalogSnapshot(cities, attractions, flights, hotels, hotelsByCity, attractionsByCity);
    }
}

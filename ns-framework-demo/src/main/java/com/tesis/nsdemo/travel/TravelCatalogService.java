package com.tesis.nsdemo.travel;

import com.tesis.nsdemo.client.FlightServiceClient;
import com.tesis.nsdemo.client.HotelServiceClient;
import com.tesis.nsdemo.client.TourismServiceClient;
import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.FlightDto;
import com.tesis.nsdemo.client.dto.HotelDto;
import com.tesis.nsframework.core.exception.ExternalServiceException;
import feign.FeignException;
import feign.RetryableException;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class TravelCatalogService {
    private static final String FLIGHT_SERVICE_NAME = "servicio mock de vuelos";
    private static final String HOTEL_SERVICE_NAME = "servicio mock de hoteles";
    private static final String TOURISM_SERVICE_NAME = "servicio mock de turismo";

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
        List<CityDto> cities = executeCatalogCall(TOURISM_SERVICE_NAME,
                "obtener la lista de ciudades",
                tourismServiceClient::getCities).stream()
                .sorted(Comparator.comparing(CityDto::id))
                .toList();
        List<AttractionDto> attractions = executeCatalogCall(TOURISM_SERVICE_NAME,
                "obtener la lista de atractivos",
                tourismServiceClient::getAttractions).stream()
                .sorted(Comparator.comparing(AttractionDto::id))
                .toList();
        List<FlightDto> flights = executeCatalogCall(FLIGHT_SERVICE_NAME,
                "obtener la lista de vuelos",
                () -> flightServiceClient.getFlights(travelDate)).stream()
                .sorted(Comparator.comparing(FlightDto::id))
                .toList();
        List<HotelDto> hotels = cities.stream()
                .flatMap(city -> executeCatalogCall(HOTEL_SERVICE_NAME,
                        "obtener la lista de hoteles para la ciudad " + city.id(),
                        () -> hotelServiceClient.getHotels(city.id(), travelDate)).stream())
                .sorted(Comparator.comparing(HotelDto::id))
                .toList();

        Map<String, List<HotelDto>> hotelsByCity = hotels.stream()
                .collect(Collectors.groupingBy(HotelDto::ciudadId, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<AttractionDto>> attractionsByCity = attractions.stream()
                .collect(Collectors.groupingBy(AttractionDto::ciudadId, LinkedHashMap::new, Collectors.toList()));

        return new TravelCatalogSnapshot(cities, attractions, flights, hotels, hotelsByCity, attractionsByCity);
    }

    private <T> List<T> executeCatalogCall(String serviceName, String operation, Supplier<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (RetryableException ex) {
            throw classifyRetryable(serviceName, operation, ex);
        } catch (FeignException ex) {
            throw classifyFeign(serviceName, operation, ex);
        } catch (RuntimeException ex) {
            throw ExternalServiceException.unavailable(serviceName,
                    "No se pudo " + operation + " desde el " + serviceName,
                    ex);
        }
    }

    private ExternalServiceException classifyRetryable(String serviceName, String operation, RetryableException ex) {
        if (isTimeout(ex)) {
            return ExternalServiceException.timeout(serviceName,
                    "El " + serviceName + " excedio el tiempo de espera al intentar " + operation,
                    ex);
        }
        return ExternalServiceException.unavailable(serviceName,
                "No se pudo conectar con el " + serviceName + " al intentar " + operation,
                ex);
    }

    private ExternalServiceException classifyFeign(String serviceName, String operation, FeignException ex) {
        if (ex.status() == 408 || ex.status() == 504) {
            return ExternalServiceException.timeout(serviceName,
                    "El " + serviceName + " excedio el tiempo de espera al intentar " + operation,
                    ex);
        }
        if (ex.status() >= 500 || ex.status() == -1) {
            return ExternalServiceException.unavailable(serviceName,
                    "El " + serviceName + " no esta disponible al intentar " + operation,
                    ex);
        }
        return ExternalServiceException.badGateway(serviceName,
                "El " + serviceName + " devolvio una respuesta invalida al intentar " + operation,
                ex);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        String message = throwable.getMessage();
        return message != null && message.toLowerCase().contains("timed out");
    }
}

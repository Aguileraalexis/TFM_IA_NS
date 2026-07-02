package com.tesis.nsdemo.impl;

import com.tesis.nsdemo.client.FlightServiceClient;
import com.tesis.nsdemo.client.HotelServiceClient;
import com.tesis.nsdemo.client.dto.FlightReservationDto;
import com.tesis.nsdemo.client.dto.FlightReservationRequest;
import com.tesis.nsdemo.client.dto.HotelReservationDto;
import com.tesis.nsdemo.client.dto.HotelReservationRequest;
import com.tesis.nsframework.core.exception.FrameworkException;
import com.tesis.nsframework.core.model.ActionOutcome;
import com.tesis.nsframework.core.model.ExecutionContext;
import com.tesis.nsframework.core.model.PlannedAction;
import com.tesis.nsframework.core.port.ActionExecutor;
import feign.FeignException;
import feign.RetryableException;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta acciones de viaje usando clientes OpenFeign contra servicios mock.
 *
 * <p>Este ejecutor lee parametros desde dos fuentes:
 * <ul>
 *   <li>{@link ExecutionContext}: id de viajero, fecha de viaje y mapeo ruta->aerolinea
 *       inyectado por {@code TravelExecutionOrchestrator} desde {@code PlanningProblem.metadata()}.</li>
 *   <li>{@link PlannedAction#arguments()}: IDs de ciudad/hotel/atractivo parseados del texto
 *       del plan PDDL. Los valores se convierten a mayusculas porque {@code PlanTextParser}
 *       transforma todo a minusculas.</li>
 * </ul>
 *
 * <p>Las acciones de vuelo usan la convencion parametrizada {@code (book-flight traveler from to)}.
 * El origen y el destino se obtienen de {@link PlannedAction#arguments()} y se elevan a mayusculas
 * porque {@code PlanTextParser} transforma todo a minusculas.
 */
@Component
public class TravelFeignActionExecutor implements ActionExecutor {

    private static final String BOOK_FLIGHT_ACTION = "book-flight";

    private final FlightServiceClient flightServiceClient;
    private final HotelServiceClient hotelServiceClient;

    public TravelFeignActionExecutor(FlightServiceClient flightServiceClient,
                                     HotelServiceClient hotelServiceClient) {
        this.flightServiceClient = flightServiceClient;
        this.hotelServiceClient = hotelServiceClient;
    }

    @Override
    public ActionOutcome execute(PlannedAction action, ExecutionContext context) {
        if (BOOK_FLIGHT_ACTION.equals(action.name())) {
            return executeFlightReservation(action, context);
        }
        return switch (action.name()) {
            case "book-hotel"        -> executeHotelReservation(action, context);
            case "visit-attraction"  -> executeAttractionVisit(action);
            default -> ActionOutcome.failure(400,
                    "Accion de viaje no soportada: " + action.name(),
                    Map.of("action", action.name()));
        };
    }

    // -------------------------------------------------------------------------
    // Vuelo
    // -------------------------------------------------------------------------

    private ActionOutcome executeFlightReservation(PlannedAction action, ExecutionContext context) {
        List<String> args = action.arguments();
        if (args.size() < 3) {
            throw new FrameworkException("book-flight requiere 3 argumentos (traveler, origin, destination), recibidos: " + args);
        }
        String originCityId = args.get(1).toUpperCase();
        String destCityId   = args.get(2).toUpperCase();

        String userId     = requiredCtx(context, "travelerId");
        String travelDate = requiredCtx(context, "travelDate");
        String airlineId  = lookupAirline(context, originCityId, destCityId);
        String travelerSymbol = requiredCtx(context, "travelerSymbol");

        FlightReservationDto reservation;
        try {
            reservation = flightServiceClient.createReservation(
                    new FlightReservationRequest(userId, airlineId, originCityId, destCityId, LocalDate.parse(travelDate)));
        } catch (FeignException ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", action.asInvocation());
            payload.put("origen", originCityId);
            payload.put("destino", destCityId);
            payload.put("status", ex.status());
            return ActionOutcome.failure(ex.status(), mensajeFeign("reserva de vuelo", ex), payload);
        } catch (RuntimeException ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", action.asInvocation());
            payload.put("origen", originCityId);
            payload.put("destino", destCityId);
            return falloServicioExterno("reserva de vuelo", ex, payload);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.id());
        payload.put("flightId",      reservation.vueloId());
        payload.put("airlineId",     reservation.aerolineaId());

        Map<String, String> effects = new LinkedHashMap<>();
        effects.put("(at " + travelerSymbol + " " + originCityId + ")", "false");
        effects.put("(at " + travelerSymbol + " " + destCityId   + ")", "true");
        effects.put("(visited-city " + destCityId + ")", "true");
        effects.put("(flight-booked " + travelerSymbol + " " + originCityId + " " + destCityId + ")", "true");
        return ActionOutcome.success(201, "Vuelo reservado correctamente", payload, effects);
    }

    // -------------------------------------------------------------------------
    // Hotel
    // -------------------------------------------------------------------------

    private ActionOutcome executeHotelReservation(PlannedAction action, ExecutionContext context) {
        // Linea de plan: (book-hotel traveler_1 ht016 pari) -> argumentos en minusculas por PlanTextParser
        List<String> args = action.arguments();
        if (args.size() < 3) {
            throw new FrameworkException("book-hotel requiere 3 argumentos (traveler, hotel, city), recibidos: " + args);
        }
        String travelerSymbol = args.get(0);
        String hotelId        = args.get(1).toUpperCase();
        String cityId         = args.get(2).toUpperCase();

        String userId     = requiredCtx(context, "travelerId");
        String travelDate = requiredCtx(context, "travelDate");

        HotelReservationDto reservation;
        try {
            reservation = hotelServiceClient.createReservation(
                    new HotelReservationRequest(userId, hotelId, LocalDate.parse(travelDate)));
        } catch (FeignException ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", action.asInvocation());
            payload.put("hotelId", hotelId);
            payload.put("ciudadId", cityId);
            payload.put("status", ex.status());
            return ActionOutcome.failure(ex.status(), mensajeFeign("reserva de hotel", ex), payload);
        } catch (RuntimeException ex) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", action.asInvocation());
            payload.put("hotelId", hotelId);
            payload.put("ciudadId", cityId);
            return falloServicioExterno("reserva de hotel", ex, payload);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.id());
        payload.put("roomId",        reservation.habitacionId());
        payload.put("hotelId",       reservation.hotelId());

        Map<String, String> effects = new LinkedHashMap<>();
        effects.put("(hotel-booked " + travelerSymbol + " " + hotelId + " " + cityId + ")", "true");
        return ActionOutcome.success(201, "Hotel reservado correctamente", payload, effects);
    }

    // -------------------------------------------------------------------------
    // Atractivo
    // -------------------------------------------------------------------------

    private ActionOutcome executeAttractionVisit(PlannedAction action) {
        // Linea de plan: (visit-attraction traveler_1 at017 pari)
        List<String> args = action.arguments();
        if (args.size() < 2) {
            throw new FrameworkException("visit-attraction requires at least 2 arguments, got: " + args);
        }
        String attractionId = args.get(1).toUpperCase();
        Map<String, String> effects = Map.of("(visited-attraction " + attractionId + ")", "true");
        return ActionOutcome.success(200, "Atractivo marcado como visitado",
                Map.of("attractionId", attractionId), effects);
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private String requiredCtx(ExecutionContext context, String key) {
        String value = context.getAsString(key);
        if (value == null) {
            throw new FrameworkException("Falta el valor de contexto '" + key + "' requerido para ejecutar la accion de viaje");
        }
        return value;
    }

    private String lookupAirline(ExecutionContext context, String originCityId, String destCityId) {
        Object raw = context.get("preferredAirlineByRoute");
        if (raw instanceof Map<?, ?> map) {
            Object airline = map.get(originCityId + "->" + destCityId);
            if (airline != null) {
                return String.valueOf(airline);
            }
        }
        throw new FrameworkException("No se encontro aerolinea para la ruta " + originCityId + " -> " + destCityId);
    }

    private String mensajeFeign(String operacion, FeignException ex) {
        String detalle = ex.contentUTF8();
        if (detalle != null && !detalle.isBlank()) {
            return "Fallo la " + operacion + ": " + detalle;
        }
        return "Fallo la " + operacion + " con status " + ex.status();
    }

    private ActionOutcome falloServicioExterno(String operacion, RuntimeException ex, Map<String, Object> payload) {
        int status = statusServicioExterno(ex);
        payload.put("status", status);
        payload.put("error", ex.getClass().getSimpleName());
        String message = status == 504
                ? "El servicio externo excedio el tiempo de espera al procesar la " + operacion
                : "El servicio externo no esta disponible para completar la " + operacion;
        return ActionOutcome.failure(status, message, payload);
    }

    private int statusServicioExterno(RuntimeException ex) {
        if (ex instanceof RetryableException retryableException && isTimeout(retryableException)) {
            return 504;
        }
        return 503;
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

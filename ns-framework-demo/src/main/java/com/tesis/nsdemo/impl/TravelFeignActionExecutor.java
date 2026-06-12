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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes travel actions using OpenFeign clients against mock services.
 *
 * <p>This executor reads parameters from two sources:
 * <ul>
 *   <li>{@link ExecutionContext} – traveler ID, travel date, and the route→airline mapping
 *       injected by {@code TravelExecutionOrchestrator} from {@code PlanningProblem.metadata()}.</li>
 *   <li>{@link PlannedAction#arguments()} – city/hotel/attraction IDs as parsed from the PDDL
 *       plan text.  Values are uppercased because {@code PlanTextParser} lowercases everything.</li>
 * </ul>
 *
 * <p>Flight actions follow the naming convention {@code book-flight-{origin}-{destination}}
 * (e.g. {@code book-flight-madr-pari}).  Origin and destination are derived by uppercasing
 * the 4-char city codes embedded in the action name.
 */
@Component
public class TravelFeignActionExecutor implements ActionExecutor {

    private static final String BOOK_FLIGHT_PREFIX = "book-flight-";

    private final FlightServiceClient flightServiceClient;
    private final HotelServiceClient hotelServiceClient;

    public TravelFeignActionExecutor(FlightServiceClient flightServiceClient,
                                     HotelServiceClient hotelServiceClient) {
        this.flightServiceClient = flightServiceClient;
        this.hotelServiceClient = hotelServiceClient;
    }

    @Override
    public ActionOutcome execute(PlannedAction action, ExecutionContext context) {
        if (action.name().startsWith(BOOK_FLIGHT_PREFIX)) {
            return executeFlightReservation(action, context);
        }
        return switch (action.name()) {
            case "book-hotel"        -> executeHotelReservation(action, context);
            case "visit-attraction"  -> executeAttractionVisit(action);
            default -> ActionOutcome.failure(400,
                    "Unsupported travel action: " + action.name(),
                    Map.of("action", action.name()));
        };
    }

    // -------------------------------------------------------------------------
    // Flight
    // -------------------------------------------------------------------------

    private ActionOutcome executeFlightReservation(PlannedAction action, ExecutionContext context) {
        // Action name pattern: book-flight-{originLower}-{destLower}
        // City IDs are 4-char uppercase codes → safe to just uppercase the tokens.
        String rest = action.name().substring(BOOK_FLIGHT_PREFIX.length()); // "madr-pari"
        String[] parts = rest.split("-", 2);
        if (parts.length != 2) {
            throw new FrameworkException("Cannot parse city codes from flight action name: " + action.name());
        }
        String originCityId = parts[0].toUpperCase();
        String destCityId   = parts[1].toUpperCase();

        String userId     = requiredCtx(context, "travelerId");
        String travelDate = requiredCtx(context, "travelDate");
        String airlineId  = lookupAirline(context, originCityId, destCityId);
        String travelerSymbol = requiredCtx(context, "travelerSymbol");

        FlightReservationDto reservation = flightServiceClient.createReservation(
                new FlightReservationRequest(userId, airlineId, originCityId, destCityId, LocalDate.parse(travelDate)));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.id());
        payload.put("flightId",      reservation.vueloId());
        payload.put("airlineId",     reservation.aerolineaId());

        Map<String, String> effects = new LinkedHashMap<>();
        effects.put("(at " + travelerSymbol + " " + originCityId + ")", "false");
        effects.put("(at " + travelerSymbol + " " + destCityId   + ")", "true");
        effects.put("(visited-city " + destCityId + ")", "true");
        effects.put("(flight-booked " + travelerSymbol + " " + originCityId + " " + destCityId + ")", "true");
        return ActionOutcome.success(201, "Flight booked successfully", payload, effects);
    }

    // -------------------------------------------------------------------------
    // Hotel
    // -------------------------------------------------------------------------

    private ActionOutcome executeHotelReservation(PlannedAction action, ExecutionContext context) {
        // Plan line: (book-hotel traveler_1 ht016 pari) → arguments lowercased by PlanTextParser
        List<String> args = action.arguments();
        if (args.size() < 3) {
            throw new FrameworkException("book-hotel requires 3 arguments (traveler, hotel, city), got: " + args);
        }
        String travelerSymbol = args.get(0);
        String hotelId        = args.get(1).toUpperCase();
        String cityId         = args.get(2).toUpperCase();

        String userId     = requiredCtx(context, "travelerId");
        String travelDate = requiredCtx(context, "travelDate");

        HotelReservationDto reservation = hotelServiceClient.createReservation(
                new HotelReservationRequest(userId, hotelId, LocalDate.parse(travelDate)));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.id());
        payload.put("roomId",        reservation.habitacionId());
        payload.put("hotelId",       reservation.hotelId());

        Map<String, String> effects = new LinkedHashMap<>();
        effects.put("(hotel-booked " + travelerSymbol + " " + hotelId + " " + cityId + ")", "true");
        return ActionOutcome.success(201, "Hotel booked successfully", payload, effects);
    }

    // -------------------------------------------------------------------------
    // Attraction
    // -------------------------------------------------------------------------

    private ActionOutcome executeAttractionVisit(PlannedAction action) {
        // Plan line: (visit-attraction traveler_1 at017 pari)
        List<String> args = action.arguments();
        if (args.size() < 2) {
            throw new FrameworkException("visit-attraction requires at least 2 arguments, got: " + args);
        }
        String attractionId = args.get(1).toUpperCase();
        Map<String, String> effects = Map.of("(visited-attraction " + attractionId + ")", "true");
        return ActionOutcome.success(200, "Attraction marked as visited",
                Map.of("attractionId", attractionId), effects);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String requiredCtx(ExecutionContext context, String key) {
        String value = context.getAsString(key);
        if (value == null) {
            throw new FrameworkException("Missing context value '" + key + "' required for travel action execution");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private String lookupAirline(ExecutionContext context, String originCityId, String destCityId) {
        Object raw = context.get("preferredAirlineByRoute");
        if (raw instanceof Map<?, ?> map) {
            Object airline = map.get(originCityId + "->" + destCityId);
            if (airline != null) {
                return String.valueOf(airline);
            }
        }
        throw new FrameworkException("No airline found for route " + originCityId + " -> " + destCityId);
    }
}

package com.tesis.nsdemo.travel;

import com.tesis.nsdemo.client.dto.AttractionDto;
import com.tesis.nsdemo.client.dto.CityDto;
import com.tesis.nsdemo.client.dto.HotelDto;
import org.springframework.stereotype.Service;

import java.util.StringJoiner;

@Service
public class TravelDomainPddlGenerator {
    public static final String DOMAIN_NAME = "travel-dynamic";

    private final TravelCatalogService travelCatalogService;

    public TravelDomainPddlGenerator(TravelCatalogService travelCatalogService) {
        this.travelCatalogService = travelCatalogService;
    }

    public String generate() {
        return generate(null);
    }

    public String generate(java.time.LocalDate travelDate) {
        TravelCatalogSnapshot snapshot = travelCatalogService.fetchSnapshot(travelDate);
        String constants = constantsBlock(snapshot);
        return "(define (domain " + DOMAIN_NAME + ")\n" +
                "  (:requirements :strips :typing)\n" +
                "  (:types traveler city hotel attraction)\n" +
                constants +
                "  (:predicates\n" +
                "    (at ?t - traveler ?c - city)\n" +
                "    (flight-available ?from - city ?to - city)\n" +
                "    (flight-booked ?t - traveler ?from - city ?to - city)\n" +
                "    (hotel-in-city ?h - hotel ?c - city)\n" +
                "    (hotel-booked ?t - traveler ?h - hotel ?c - city)\n" +
                "    (attraction-in-city ?a - attraction ?c - city)\n" +
                "    (visited-city ?c - city)\n" +
                "    (visited-attraction ?a - attraction)\n" +
                "  )\n\n" +
                genericFlightAction() +
                "  (:action book-hotel\n" +
                "    :parameters (?t - traveler ?h - hotel ?c - city)\n" +
                "    :precondition (and (at ?t ?c) (hotel-in-city ?h ?c))\n" +
                "    :effect (hotel-booked ?t ?h ?c)\n" +
                "  )\n\n" +
                "  (:action visit-attraction\n" +
                "    :parameters (?t - traveler ?a - attraction ?c - city)\n" +
                "    :precondition (and (at ?t ?c) (attraction-in-city ?a ?c))\n" +
                "    :effect (visited-attraction ?a)\n" +
                "  )\n" +
                ")\n";
    }

    private String genericFlightAction() {
        return "  (:action book-flight\n" +
                "    :parameters (?t - traveler ?from - city ?to - city)\n" +
                "    :precondition (and (at ?t ?from) (flight-available ?from ?to))\n" +
                "    :effect (and\n" +
                "      (not (at ?t ?from))\n" +
                "      (at ?t ?to)\n" +
                "      (visited-city ?to)\n" +
                "      (flight-booked ?t ?from ?to)\n" +
                "    )\n" +
                "  )\n\n";
    }

    private String constantsBlock(TravelCatalogSnapshot snapshot) {
        StringJoiner cityJoiner = new StringJoiner(" ");
        for (CityDto city : snapshot.cities()) {
            cityJoiner.add(city.id());
        }
        StringJoiner hotelJoiner = new StringJoiner(" ");
        for (HotelDto hotel : snapshot.hotels()) {
            hotelJoiner.add(hotel.id());
        }
        StringJoiner attractionJoiner = new StringJoiner(" ");
        for (AttractionDto attraction : snapshot.attractions()) {
            attractionJoiner.add(attraction.id());
        }

        return "  (:constants\n" +
                "    " + cityJoiner + " - city\n" +
                "    " + hotelJoiner + " - hotel\n" +
                "    " + attractionJoiner + " - attraction\n" +
                "  )\n\n";
    }
}



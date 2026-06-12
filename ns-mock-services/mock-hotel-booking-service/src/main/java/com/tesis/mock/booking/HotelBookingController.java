package com.tesis.mock.booking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.mock.booking.dto.CiudadDto;
import com.tesis.mock.booking.dto.EstadoReserva;
import com.tesis.mock.booking.dto.HabitacionDto;
import com.tesis.mock.booking.dto.HotelDto;
import com.tesis.mock.booking.dto.ReservaDto;
import com.tesis.mock.booking.dto.UsuarioDto;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class HotelBookingController {

    private final ObjectMapper objectMapper;

    private final Map<String, CiudadDto> ciudades = new ConcurrentHashMap<>();
    private final Map<String, HotelDto> hoteles = new ConcurrentHashMap<>();
    private final Map<String, HabitacionDto> habitaciones = new ConcurrentHashMap<>();
    private final Map<String, UsuarioDto> usuarios = new ConcurrentHashMap<>();
    private final Map<String, ReservaDto> reservas = new ConcurrentHashMap<>();

    public HotelBookingController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initData() {
        cargarCiudades();
        cargarHoteles();
        cargarHabitaciones();
        cargarUsuarios();
        cargarReservas();
    }

    @GetMapping("/ciudades")
    public List<CiudadDto> obtenerCiudades() {
        return ordenar(ciudades.values().stream().toList(), CiudadDto::id);
    }

    @GetMapping("/hoteles")
    public List<HotelDto> obtenerHoteles(@RequestParam(required = false) String ciudadId) {
        if (ciudadId != null && !ciudades.containsKey(ciudadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciudad no encontrada");
        }
        return hoteles.values().stream()
                .filter(hotel -> ciudadId == null || hotel.ciudadId().equals(ciudadId))
                .sorted(Comparator.comparing(HotelDto::id))
                .toList();
    }

    @GetMapping("/reservas")
    public List<ReservaDto> obtenerReservas() {
        return ordenar(reservas.values().stream().toList(), ReservaDto::id);
    }

    @GetMapping("/reservas/{reservaId}")
    public ReservaDto obtenerReserva(@PathVariable String reservaId) {
        ReservaDto reserva = reservas.get(reservaId);
        if (reserva == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada");
        }
        return reserva;
    }

    @PostMapping("/reservas")
    public ReservaDto crearReserva(@RequestBody ReservaRequestDto request) {
        if (request == null || esVacio(request.usuarioId()) || esVacio(request.hotelId()) || request.fecha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuarioId, hotelId y fecha son obligatorios");
        }

        if (!usuarios.containsKey(request.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        HotelDto hotel = hoteles.get(request.hotelId());
        if (hotel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel no encontrado");
        }

        HabitacionDto habitacionLibre = buscarPrimeraHabitacionLibre(hotel.id(), request.fecha());
        if (habitacionLibre == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay habitaciones disponibles para el hotel en la fecha solicitada");
        }

        synchronized (reservas) {
            HabitacionDto libreConLock = buscarPrimeraHabitacionLibre(hotel.id(), request.fecha());
            if (libreConLock == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay habitaciones disponibles para el hotel en la fecha solicitada");
            }

            ReservaDto reserva = new ReservaDto(
                    "RS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    request.usuarioId(),
                    libreConLock.id(),
                    hotel.id(),
                    EstadoReserva.BOOKED,
                    request.fecha()
            );
            reservas.put(reserva.id(), reserva);
            return reserva;
        }
    }

    private HabitacionDto buscarPrimeraHabitacionLibre(String hotelId, LocalDate fecha) {
        return habitaciones.values().stream()
                .filter(habitacion -> habitacion.hotelId().equals(hotelId))
                .sorted(Comparator.comparing(HabitacionDto::id))
                .filter(habitacion -> !estaReservada(habitacion.id(), fecha))
                .findFirst()
                .orElse(null);
    }

    private boolean estaReservada(String habitacionId, LocalDate fecha) {
        return reservas.values().stream()
                .filter(reserva -> reserva.habitacionId().equals(habitacionId))
                .filter(reserva -> reserva.fecha().equals(fecha))
                .anyMatch(reserva -> reserva.estado() == EstadoReserva.BOOKED);
    }

    private void cargarCiudades() {
        List<CiudadDto> lista = leerLista("data/ciudades.json", new TypeReference<>() {
        });
        ciudades.clear();
        lista.forEach(ciudad -> guardarUnico(ciudades, ciudad.id(), ciudad, "ciudad"));
    }

    private void cargarHoteles() {
        List<HotelDto> lista = leerLista("data/hoteles.json", new TypeReference<>() {
        });
        hoteles.clear();
        for (HotelDto hotel : lista) {
            if (!ciudades.containsKey(hotel.ciudadId())) {
                throw new IllegalStateException("Hotel " + hotel.id() + " referencia ciudad inexistente");
            }
            guardarUnico(hoteles, hotel.id(), hotel, "hotel");
        }
    }

    private void cargarHabitaciones() {
        List<HabitacionDto> lista = leerLista("data/habitaciones.json", new TypeReference<>() {
        });
        habitaciones.clear();
        for (HabitacionDto habitacion : lista) {
            if (!hoteles.containsKey(habitacion.hotelId())) {
                throw new IllegalStateException("Habitacion " + habitacion.id() + " referencia hotel inexistente");
            }
            guardarUnico(habitaciones, habitacion.id(), habitacion, "habitacion");
        }
    }

    private void cargarUsuarios() {
        List<UsuarioDto> lista = leerLista("data/usuarios.json", new TypeReference<>() {
        });
        usuarios.clear();
        lista.forEach(usuario -> guardarUnico(usuarios, usuario.id(), usuario, "usuario"));
    }

    private void cargarReservas() {
        List<ReservaDto> lista = leerLista("data/reservas.json", new TypeReference<>() {
        });
        reservas.clear();
        for (ReservaDto reserva : lista) {
            if (!usuarios.containsKey(reserva.usuarioId())) {
                throw new IllegalStateException("Reserva " + reserva.id() + " referencia usuario inexistente");
            }
            if (!hoteles.containsKey(reserva.hotelId())) {
                throw new IllegalStateException("Reserva " + reserva.id() + " referencia hotel inexistente");
            }
            if (!habitaciones.containsKey(reserva.habitacionId())) {
                throw new IllegalStateException("Reserva " + reserva.id() + " referencia habitacion inexistente");
            }
            guardarUnico(reservas, reserva.id(), reserva, "reserva");
        }
    }

    private <T> List<T> leerLista(String resourcePath, TypeReference<List<T>> typeReference) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("No se encontro el recurso: " + resourcePath);
            }
            return objectMapper.readValue(input, typeReference);
        } catch (Exception ex) {
            throw new IllegalStateException("Error leyendo " + resourcePath + ": " + ex.getMessage(), ex);
        }
    }

    private <T> void guardarUnico(Map<String, T> destino, String clave, T valor, String tipo) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("Clave vacia en " + tipo);
        }
        if (destino.putIfAbsent(clave, valor) != null) {
            throw new IllegalStateException("Clave duplicada para " + tipo + ": " + clave);
        }
    }

    private <T> List<T> ordenar(List<T> lista, java.util.function.Function<T, String> keyExtractor) {
        List<T> copia = new ArrayList<>(lista);
        copia.sort(Comparator.comparing(keyExtractor));
        return copia;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    public record ReservaRequestDto(String usuarioId, String hotelId, LocalDate fecha) {
    }
}


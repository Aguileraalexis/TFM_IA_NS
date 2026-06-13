package com.tesis.mock.flight;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.mock.flight.dto.AerolineaDto;
import com.tesis.mock.flight.dto.CiudadDto;
import com.tesis.mock.flight.dto.EstadoReservaVuelo;
import com.tesis.mock.flight.dto.ReservaVueloDto;
import com.tesis.mock.flight.dto.UsuarioDto;
import com.tesis.mock.flight.dto.VueloDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Flight Booking", description = "Consulta de ciudades, aerolíneas, vuelos y gestión de reservas de vuelos")
public class FlightBookingController {

    private static final int MAX_RESERVAS_POR_VUELO_Y_FECHA = 5;

    private final ObjectMapper objectMapper;

    private final Map<String, CiudadDto> ciudades = new ConcurrentHashMap<>();
    private final Map<String, AerolineaDto> aerolineas = new ConcurrentHashMap<>();
    private final Map<String, VueloDto> vuelos = new ConcurrentHashMap<>();
    private final Map<String, UsuarioDto> usuarios = new ConcurrentHashMap<>();
    private final Map<String, ReservaVueloDto> reservas = new ConcurrentHashMap<>();

    public FlightBookingController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initData() {
        cargarCiudades();
        cargarAerolineas();
        cargarVuelos();
        cargarUsuarios();
        cargarReservas();
    }

    @Operation(summary = "Listar ciudades", description = "Devuelve todas las ciudades disponibles ordenadas por ID")
    @GetMapping("/ciudades")
    public List<CiudadDto> obtenerCiudades() {
        return ordenar(ciudades.values().stream().toList(), CiudadDto::id);
    }

    @Operation(summary = "Listar aerolíneas", description = "Devuelve todas las aerolíneas disponibles")
    @GetMapping("/aerolineas")
    public List<AerolineaDto> obtenerAerolineas() {
        return ordenar(aerolineas.values().stream().toList(), AerolineaDto::id);
    }

    @Operation(summary = "Listar usuarios", description = "Devuelve todos los usuarios registrados")
    @GetMapping("/usuarios")
    public List<UsuarioDto> obtenerUsuarios() {
        return ordenar(usuarios.values().stream().toList(), UsuarioDto::id);
    }

    @Operation(summary = "Listar vuelos", description = "Devuelve vuelos filtrados opcionalmente por ciudad origen, destino y aerolínea")
    @GetMapping("/vuelos")
    public List<VueloDto> obtenerVuelos(
            @Parameter(description = "ID ciudad de origen") @RequestParam(name = "ciudadOrigenId", required = false) String ciudadOrigenId,
            @Parameter(description = "ID ciudad de destino") @RequestParam(name = "ciudadDestinoId", required = false) String ciudadDestinoId,
            @Parameter(description = "ID de la aerolínea") @RequestParam(name = "aerolineaId", required = false) String aerolineaId
    ) {
        return vuelos.values().stream()
                .filter(vuelo -> ciudadOrigenId == null || vuelo.ciudadOrigenId().equals(ciudadOrigenId))
                .filter(vuelo -> ciudadDestinoId == null || vuelo.ciudadDestinoId().equals(ciudadDestinoId))
                .filter(vuelo -> aerolineaId == null || vuelo.aerolineaId().equals(aerolineaId))
                .sorted(Comparator.comparing(VueloDto::id))
                .toList();
    }

    @Operation(summary = "Crear reserva de vuelo", description = "Crea una reserva si existe ruta y hay plazas disponibles (máx. 5 por vuelo y fecha)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reserva creada"),
                    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
                    @ApiResponse(responseCode = "404", description = "Usuario, ciudad o aerolínea no encontrada"),
                    @ApiResponse(responseCode = "409", description = "Sin ruta disponible o vuelo completo")
            })
    @PostMapping("/reservas-vuelos")
    public ReservaVueloDto crearReserva(@RequestBody ReservaVueloRequestDto request) {
        if (request == null || esVacio(request.usuarioId()) || esVacio(request.aerolineaId()) || esVacio(request.ciudadOrigenId())
                || esVacio(request.ciudadDestinoId()) || request.fecha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "usuarioId, aerolineaId, ciudadOrigenId, ciudadDestinoId y fecha son obligatorios");
        }

        if (!usuarios.containsKey(request.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        if (!ciudades.containsKey(request.ciudadOrigenId()) || !ciudades.containsKey(request.ciudadDestinoId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciudad origen o destino no existe");
        }
        if (!aerolineas.containsKey(request.aerolineaId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aerolinea no encontrada");
        }

        VueloDto vuelo = buscarVuelo(request.ciudadOrigenId(), request.ciudadDestinoId(), request.aerolineaId());
        if (vuelo == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No existe ruta para esa aerolinea entre esas ciudades");
        }

        synchronized (reservas) {
            long ocupacion = reservas.values().stream()
                    .filter(r -> r.vueloId().equals(vuelo.id()))
                    .filter(r -> r.fecha().equals(request.fecha()))
                    .filter(r -> r.estado() == EstadoReservaVuelo.BOOKED)
                    .count();
            if (ocupacion >= MAX_RESERVAS_POR_VUELO_Y_FECHA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El vuelo ya alcanzo el limite de 5 reservas para esa fecha");
            }

            ReservaVueloDto reserva = new ReservaVueloDto(
                    "RV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    request.usuarioId(),
                    vuelo.id(),
                    vuelo.aerolineaId(),
                    vuelo.ciudadOrigenId(),
                    vuelo.ciudadDestinoId(),
                    request.fecha(),
                    EstadoReservaVuelo.BOOKED
            );
            reservas.put(reserva.id(), reserva);
            return reserva;
        }
    }

    private void cargarUsuarios() {
        List<UsuarioDto> lista = leerLista("data/usuarios.json", new TypeReference<>() {
        });
        usuarios.clear();
        lista.forEach(usuario -> guardarUnico(usuarios, usuario.id(), usuario, "usuario"));
    }

    @Operation(summary = "Obtener reserva de vuelo por ID", responses = {
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    @GetMapping("/reservas-vuelos/{reservaId}")
    public ReservaVueloDto obtenerReserva(@PathVariable("reservaId") String reservaId) {
        ReservaVueloDto reserva = reservas.get(reservaId);
        if (reserva == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada");
        }
        return reserva;
    }

    @Operation(summary = "Listar reservas de vuelos", description = "Devuelve todas las reservas de vuelo existentes")
    @GetMapping("/reservas-vuelos")
    public List<ReservaVueloDto> obtenerReservas() {
        return ordenar(reservas.values().stream().toList(), ReservaVueloDto::id);
    }

    private void cargarCiudades() {
        List<CiudadDto> lista = leerLista("data/ciudades.json", new TypeReference<>() {
        });
        ciudades.clear();
        lista.forEach(ciudad -> guardarUnico(ciudades, ciudad.id(), ciudad, "ciudad"));
    }

    private void cargarAerolineas() {
        List<AerolineaDto> lista = leerLista("data/aerolineas.json", new TypeReference<>() {
        });
        aerolineas.clear();
        lista.forEach(aerolinea -> guardarUnico(aerolineas, aerolinea.id(), aerolinea, "aerolinea"));
    }

    private void cargarVuelos() {
        List<VueloDto> lista = leerLista("data/vuelos.json", new TypeReference<>() {
        });
        vuelos.clear();
        for (VueloDto vuelo : lista) {
            if (!ciudades.containsKey(vuelo.ciudadOrigenId()) || !ciudades.containsKey(vuelo.ciudadDestinoId())) {
                throw new IllegalStateException("Vuelo " + vuelo.id() + " referencia ciudades inexistentes");
            }
            if (!aerolineas.containsKey(vuelo.aerolineaId())) {
                throw new IllegalStateException("Vuelo " + vuelo.id() + " referencia aerolinea inexistente");
            }
            guardarUnico(vuelos, vuelo.id(), vuelo, "vuelo");
        }
    }

    private void cargarReservas() {
        List<ReservaVueloDto> lista = leerLista("data/reservas-vuelos.json", new TypeReference<>() {
        });
        reservas.clear();
        for (ReservaVueloDto reserva : lista) {
            if (!usuarios.containsKey(reserva.usuarioId())) {
                throw new IllegalStateException("Reserva " + reserva.id() + " referencia usuario inexistente");
            }
            if (!vuelos.containsKey(reserva.vueloId())) {
                throw new IllegalStateException("Reserva " + reserva.id() + " referencia vuelo inexistente");
            }
            guardarUnico(reservas, reserva.id(), reserva, "reserva");
        }
    }

    private VueloDto buscarVuelo(String origenId, String destinoId, String aerolineaId) {
        return vuelos.values().stream()
                .filter(v -> v.ciudadOrigenId().equals(origenId))
                .filter(v -> v.ciudadDestinoId().equals(destinoId))
                .filter(v -> v.aerolineaId().equals(aerolineaId))
                .findFirst()
                .orElse(null);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
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

    public record ReservaVueloRequestDto(String usuarioId, String aerolineaId, String ciudadOrigenId, String ciudadDestinoId, LocalDate fecha) {
    }
}

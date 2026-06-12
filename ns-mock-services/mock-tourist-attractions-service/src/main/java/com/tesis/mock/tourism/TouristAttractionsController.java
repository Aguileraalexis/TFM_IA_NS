package com.tesis.mock.tourism;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesis.mock.tourism.dto.AtractivoTuristicoDto;
import com.tesis.mock.tourism.dto.CiudadDto;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class TouristAttractionsController {

    private final ObjectMapper objectMapper;

    private final Map<String, CiudadDto> ciudades = new ConcurrentHashMap<>();
    private final Map<String, AtractivoTuristicoDto> atractivos = new ConcurrentHashMap<>();

    public TouristAttractionsController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initData() {
        cargarCiudades();
        cargarAtractivos();
    }

    @GetMapping("/ciudades")
    public List<CiudadDto> obtenerCiudades() {
        return ordenar(ciudades.values().stream().toList(), CiudadDto::id);
    }

    @GetMapping("/atractivos")
    public List<AtractivoTuristicoDto> obtenerAtractivos() {
        return ordenar(atractivos.values().stream().toList(), AtractivoTuristicoDto::id);
    }

    @GetMapping("/ciudades/{ciudadId}/atractivos")
    public List<AtractivoTuristicoDto> obtenerAtractivosPorCiudad(@PathVariable String ciudadId) {
        if (!ciudades.containsKey(ciudadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciudad no encontrada");
        }
        return atractivos.values().stream()
                .filter(atractivo -> atractivo.ciudadId().equals(ciudadId))
                .sorted(Comparator.comparing(AtractivoTuristicoDto::id))
                .toList();
    }

    private void cargarCiudades() {
        List<CiudadDto> lista = leerLista("data/ciudades.json", new TypeReference<>() {
        });
        ciudades.clear();
        lista.forEach(ciudad -> guardarUnico(ciudades, ciudad.id(), ciudad, "ciudad"));
    }

    private void cargarAtractivos() {
        List<AtractivoTuristicoDto> lista = leerLista("data/atractivos.json", new TypeReference<>() {
        });
        atractivos.clear();
        for (AtractivoTuristicoDto atractivo : lista) {
            if (!ciudades.containsKey(atractivo.ciudadId())) {
                throw new IllegalStateException("Atractivo " + atractivo.id() + " referencia ciudad inexistente");
            }
            guardarUnico(atractivos, atractivo.id(), atractivo, "atractivo");
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
}


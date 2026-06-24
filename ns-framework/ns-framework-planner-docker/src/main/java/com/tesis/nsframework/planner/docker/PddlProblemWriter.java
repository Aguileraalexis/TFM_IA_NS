package com.tesis.nsframework.planner.docker;

import com.tesis.nsframework.core.model.PlanningProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

public class PddlProblemWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PddlProblemWriter.class);

    public String write(PlanningProblem problem) {
        StringJoiner objectsJoiner = new StringJoiner("\n    ", "(:objects\n    ", "\n  )");
        problem.objects().forEach(obj -> objectsJoiner.add(sanitizeString(obj, "object")));

        StringJoiner initJoiner = new StringJoiner("\n    ", "(:init\n    ", "\n  )");
        problem.initFacts().forEach(fact -> initJoiner.add(sanitizeString(fact, "fact")));

        return "(define (problem " + sanitizeString(problem.problemName(), "problem-name") + ")\n" +
                "  (:domain " + sanitizeString(problem.domainName(), "domain-name") + ")\n" +
                "  " + objectsJoiner + "\n" +
                "  " + initJoiner + "\n" +
                "  (:goal\n    " + sanitizeString(problem.goalExpression(), "goal") + "\n  )\n" +
                ")\n";
    }

    private String sanitizeString(String value, String context) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // Valida la codificacion UTF-8
        try {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            String reencoded = new String(bytes, StandardCharsets.UTF_8);
            if (!reencoded.equals(value)) {
                LOGGER.warn("Se detecto un problema de codificacion en {}: longitud original={}, tras recodificar={}", 
                    context, value.length(), reencoded.length());
                return reencoded;
            }
        } catch (Exception ex) {
            LOGGER.error("Error al procesar el valor de {} durante la validacion UTF-8", context, ex);
            // Elimina caracteres problematicos y devuelve una cadena saneada
            return value.replaceAll("[^\\x00-\\x7F]", "?");
        }
        
        return value;
    }
}

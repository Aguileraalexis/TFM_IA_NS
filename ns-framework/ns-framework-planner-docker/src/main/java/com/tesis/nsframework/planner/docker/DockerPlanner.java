package com.tesis.nsframework.planner.docker;

import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.port.Planner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class DockerPlanner implements Planner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerPlanner.class);
    private static final int MAX_LOG_SNIPPET_LENGTH = 4_000;

    private final String dockerImage;
    private final String containerCommand;
    private final PddlProblemWriter problemWriter = new PddlProblemWriter();
    private final PlanTextParser planTextParser = new PlanTextParser();

    public DockerPlanner(String dockerImage, String containerCommand) {
        this.dockerImage = dockerImage;
        this.containerCommand = containerCommand == null || containerCommand.isBlank()
                ? "--plan-file /planner/plan.txt /planner/domain.pddl /planner/problem.pddl --search 'astar(lmcut())'"
                : containerCommand;
    }

    @Override
    public PlanResult plan(String domainPddl, PlanningProblem problem, PlannerOptions options) {
        Duration timeout = options == null || options.timeout() == null ? Duration.ofSeconds(30) : options.timeout();
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("ns-framework-planner");
            Path plannerDir = Files.createDirectories(workDir.resolve("planner"));
            Path domainFile = plannerDir.resolve("domain.pddl");
            Path problemFile = plannerDir.resolve("problem.pddl");
            Path outputFile = plannerDir.resolve("plan.txt");

            String normalisedDomain = normalisePddl(domainPddl);
            validateAndSanitizeContent(normalisedDomain, "domain");
            Files.writeString(domainFile, normalisedDomain, StandardCharsets.UTF_8);

            String problemContent = normalisePddl(problemWriter.write(problem));
            validateAndSanitizeContent(problemContent, "problem");
            Files.writeString(problemFile, problemContent, StandardCharsets.UTF_8);

            if (LOGGER.isDebugEnabled()) {
                byte[] domainBytes = Files.readAllBytes(domainFile);
                LOGGER.debug("domain.pddl written: {} bytes (on disk), first 200 chars: {}",
                        domainBytes.length, abbreviate(normalisedDomain, 200));

                byte[] problemBytes = Files.readAllBytes(problemFile);
                LOGGER.debug("problem.pddl written: {} bytes (on disk), first 200 chars: {}",
                        problemBytes.length, abbreviate(problemContent, 200));

                StringBuilder hexDump = new StringBuilder();
                int maxBytesToDump = Math.min(150, problemBytes.length);
                for (int i = 0; i < maxBytesToDump; i++) {
                    hexDump.append(String.format("%02X ", problemBytes[i]));
                    if ((i + 1) % 16 == 0) {
                        hexDump.append("\n");
                    }
                }
                LOGGER.debug("problem.pddl hex dump (first 150 bytes):\n{}", hexDump);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("domain.pddl completo:\n{}", normalisedDomain);
                LOGGER.trace("problem.pddl completo:\n{}", problemContent);
            }

            String hostPlannerPath = plannerDir.toAbsolutePath().toString().replace('\\', '/');
            List<String> command = buildDockerCommand(hostPlannerPath);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Verifying PDDL files before Docker invocation:");
                LOGGER.debug("  domain.pddl exists: {} ({} bytes)", Files.exists(domainFile),
                        Files.exists(domainFile) ? Files.size(domainFile) : 0);
                LOGGER.debug("  problem.pddl exists: {} ({} bytes)", Files.exists(problemFile),
                        Files.exists(problemFile) ? Files.size(problemFile) : 0);
            }

            LOGGER.info("Ejecutando planificador Docker: {}", command);

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            CompletableFuture<String> processOutputFuture = readProcessOutputAsync(process);

            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                String processOutput = awaitProcessOutput(processOutputFuture);
                LOGGER.error("El planificador supero el tiempo limite de {} segundos. command={}, workDir={}, output={}",
                        timeout.toSeconds(), command, plannerDir, abbreviate(processOutput));
                return PlanResult.failure("El planificador supero el tiempo limite de " + timeout.toSeconds()
                        + " segundos. Detalle: " + abbreviate(processOutput));
            }

            int exitCode = process.exitValue();
            String processOutput = awaitProcessOutput(processOutputFuture);
            if (exitCode != 0 && !Files.exists(outputFile)) {
                LOGGER.error("El planificador finalizo con codigo {}. command={}, workDir={}, output={}",
                        exitCode, command, plannerDir, abbreviate(processOutput));
                return PlanResult.failure("El planificador finalizo con codigo " + exitCode
                        + ". Detalle: " + abbreviate(processOutput));
            }
            if (exitCode != 0) {
                LOGGER.warn("El planificador finalizo con codigo {} pero genero plan. command={}, workDir={}, output={}",
                        exitCode, command, plannerDir, abbreviate(processOutput));
            } else if (!processOutput.isBlank()) {
                LOGGER.debug("Salida del planificador. command={}, workDir={}, output={}",
                        command, plannerDir, abbreviate(processOutput));
            }

            String rawPlan = Files.exists(outputFile) ? Files.readString(outputFile, StandardCharsets.UTF_8) : "";
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Plan devuelto por el planner:\n{}", abbreviate(rawPlan));
            }
            return PlanResult.success(planTextParser.parse(rawPlan), rawPlan);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("Error al ejecutar el planificador", ex);
            return PlanResult.failure("Fallo la ejecucion del planificador: " + ex.getMessage());
        } catch (IOException ex) {
            LOGGER.error("Error al ejecutar el planificador", ex);
            return PlanResult.failure("Fallo la ejecucion del planificador: " + ex.getMessage());
        } finally {
            if (workDir != null) {
                try (Stream<Path> paths = Files.walk(workDir)) {
                    paths
                            .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
                } catch (IOException ignored) {
                }
            }
        }
    }

    private List<String> buildDockerCommand(String hostPlannerPath) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        // --mount evita la ambigüedad de la letra de unidad de Windows en la sintaxis -v C:/...:/planner.
        command.add("--mount");
        command.add("type=bind,source=" + hostPlannerPath + ",target=/planner");
        command.add(dockerImage);

        List<String> plannerCommand = stripRedundantEntrypoint(splitShellLike(containerCommand));
        if (plannerCommand.isEmpty()) {
            throw new IllegalArgumentException("containerCommand no puede estar vacio");
        }
        command.addAll(plannerCommand);
        return command;
    }

    private List<String> stripRedundantEntrypoint(List<String> plannerCommand) {
        if (plannerCommand.isEmpty()) {
            return plannerCommand;
        }

        String first = plannerCommand.getFirst();
        if ("fast-downward.py".equals(first) || "/workspace/downward/fast-downward.py".equals(first)) {
            return new ArrayList<>(plannerCommand.subList(1, plannerCommand.size()));
        }
        return plannerCommand;
    }

    // Separador minimalista estilo shell que conserva fragmentos entre comillas como un solo argumento.
    private List<String> splitShellLike(String raw) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }
            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }
            if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    /**
     * Elimina caracteres carriage return para que el archivo use siempre finales de línea Unix LF.
     * El lector en Python de Fast Downward abre los archivos PDDL con el códec por defecto (UTF-8);
     * un byte 0x0D (\r) suelto o cualquier byte Windows-1252 que se cuele disparará un
     * UnicodeDecodeError. Quitar \r es seguro porque PDDL usa solo espacios en blanco para
     * separar tokens.
     */
    private static String normalisePddl(String content) {
        if (content == null) return "";
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    private void validateAndSanitizeContent(String content, String fileType) {
        try {
            byte[] encoded = content.getBytes(StandardCharsets.UTF_8);
            String decoded = new String(encoded, StandardCharsets.UTF_8);
            if (!decoded.equals(content)) {
                LOGGER.warn("Se detecto una discrepancia de codificacion en {}", fileType);
            }
        } catch (Exception ex) {
            LOGGER.error("Error al validar la codificacion del contenido de {}: {}", fileType, ex.getMessage());
            throw new IllegalArgumentException("Se detecto una codificacion de contenido invalida en " + fileType, ex);
        }
    }

    private CompletableFuture<String> readProcessOutputAsync(Process process) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream inputStream = process.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8_192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                return output.toString(StandardCharsets.UTF_8).trim();
            } catch (IOException ex) {
                LOGGER.warn("No se pudo leer la salida del planificador", ex);
                return "";
            }
        });
    }

    private String awaitProcessOutput(CompletableFuture<String> processOutputFuture) {
        try {
            return processOutputFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Se interrumpio la espera de la salida del planificador", ex);
            return "";
        } catch (ExecutionException | TimeoutException ex) {
            LOGGER.warn("No se pudo recuperar la salida completa del planificador", ex);
            return "";
        }
    }

    private String abbreviate(String text) {
        return abbreviate(text, MAX_LOG_SNIPPET_LENGTH);
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null || text.isBlank()) {
            return "(sin salida del proceso)";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "... [truncated]";
    }
}

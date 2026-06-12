package com.tesis.nsframework.planner.docker;

import com.tesis.nsframework.core.model.PlanResult;
import com.tesis.nsframework.core.model.PlannerOptions;
import com.tesis.nsframework.core.model.PlanningProblem;
import com.tesis.nsframework.core.port.Planner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DockerPlanner implements Planner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerPlanner.class);

    private final String dockerImage;
    private final String containerCommand;
    private final PddlProblemWriter problemWriter = new PddlProblemWriter();
    private final PlanTextParser planTextParser = new PlanTextParser();

    public DockerPlanner(String dockerImage, String containerCommand) {
        this.dockerImage = dockerImage;
        this.containerCommand = containerCommand == null || containerCommand.isBlank()
                ? "planner/domain.pddl planner/problem.pddl"
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

            Files.writeString(domainFile, domainPddl, StandardCharsets.UTF_8);
            Files.writeString(problemFile, problemWriter.write(problem), StandardCharsets.UTF_8);

            List<String> command = List.of(
                    "docker", "run", "--rm",
                    "-v", plannerDir.toAbsolutePath() + ":/planner",
                    dockerImage,
                    "sh", "-lc",
                    containerCommand + " > /planner/plan.txt"
            );

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return PlanResult.failure("Planner timed out after " + timeout.toSeconds() + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0 && !Files.exists(outputFile)) {
                return PlanResult.failure("Planner exited with code " + exitCode);
            }

            String rawPlan = Files.exists(outputFile) ? Files.readString(outputFile, StandardCharsets.UTF_8) : "";
            return PlanResult.success(planTextParser.parse(rawPlan), rawPlan);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("Failed to execute planner", ex);
            return PlanResult.failure("Planner execution failed: " + ex.getMessage());
        } finally {
            if (workDir != null) {
                try {
                    Files.walk(workDir)
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
}

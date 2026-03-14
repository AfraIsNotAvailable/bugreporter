package com.group11.bugreporter.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DockerComposeBootstrap {

    private static final Logger LOGGER = Logger.getLogger(DockerComposeBootstrap.class.getName());

    private final Settings settings;
    private final CommandExecutor commandExecutor;
    private final PortProbe portProbe;

    DockerComposeBootstrap(Settings settings, CommandExecutor commandExecutor, PortProbe portProbe) {
        this.settings = settings;
        this.commandExecutor = commandExecutor;
        this.portProbe = portProbe;
    }

    public static void bootstrapIfNecessary() {
        Settings settings = Settings.fromApplicationProperties();
        DockerComposeBootstrap bootstrap = new DockerComposeBootstrap(
                settings,
                new ProcessCommandExecutor(),
                new SocketPortProbe());
        bootstrap.ensureReady();
    }

    void ensureReady() {
        if (!settings.enabled()) {
            LOGGER.info("Docker bootstrap is disabled (app.startup.docker.enabled=false).");
            return;
        }

        if (dependenciesUp()) {
            LOGGER.info("Required local dependencies are already reachable.");
            return;
        }

        Path composeDirectory = findComposeDirectory(Paths.get(System.getProperty("user.dir")));
        if (composeDirectory == null) {
            throw new IllegalStateException("Dependencies are down and no docker-compose.yml was found from user.dir="
                    + System.getProperty("user.dir"));
        }

        LOGGER.info("Local dependencies are down. Starting Docker services from " + composeDirectory + " ...");
        runComposeUp(composeDirectory);

        if (!waitForDependencies()) {
            throw new IllegalStateException("Docker services started but dependencies did not become ready in "
                    + settings.waitTimeout().toSeconds() + "s. Required ports=" + settings.requiredPorts());
        }

        LOGGER.info("Docker dependencies are ready.");
    }

    private boolean dependenciesUp() {
        for (int port : settings.requiredPorts()) {
            if (!portProbe.isOpen("localhost", port, settings.connectTimeout())) {
                return false;
            }
        }
        return true;
    }

    private boolean waitForDependencies() {
        Instant deadline = Instant.now().plus(settings.waitTimeout());
        while (Instant.now().isBefore(deadline)) {
            if (dependenciesUp()) {
                return true;
            }

            try {
                Thread.sleep(settings.pollInterval().toMillis());
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return dependenciesUp();
    }

    private void runComposeUp(Path composeDirectory) {
        List<List<String>> candidates = List.of(
                List.of("docker", "compose", "up", "-d"),
                List.of("docker-compose", "up", "-d"));

        List<String> errors = new ArrayList<>();
        for (List<String> command : candidates) {
            try {
                CommandResult result = commandExecutor.execute(composeDirectory, command, settings.composeCommandTimeout());
                if (result.exitCode() == 0) {
                    LOGGER.info("Executed: " + String.join(" ", command));
                    if (!result.output().isBlank()) {
                        LOGGER.info(result.output());
                    }
                    return;
                }

                String message = "Command failed (" + String.join(" ", command) + ") with exit code "
                        + result.exitCode() + ": " + result.output();
                errors.add(message);
                LOGGER.warning(message);
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                String message = "Command unavailable or failed (" + String.join(" ", command) + "): "
                        + exception.getMessage();
                errors.add(message);
                LOGGER.log(Level.WARNING, message, exception);
            }
        }

        throw new IllegalStateException("Unable to start docker dependencies. Details: " + String.join(" | ", errors));
    }

    private static Path findComposeDirectory(Path startDirectory) {
        Path current = startDirectory;
        while (current != null) {
            if (Files.exists(current.resolve("docker-compose.yml"))
                    || Files.exists(current.resolve("docker-compose.yaml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    public record Settings(
            boolean enabled,
            List<Integer> requiredPorts,
            Duration connectTimeout,
            Duration waitTimeout,
            Duration pollInterval,
            Duration composeCommandTimeout
    ) {

        public static Settings fromApplicationProperties() {
            Properties properties = new Properties();
            try (InputStream inputStream = DockerComposeBootstrap.class
                    .getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (inputStream != null) {
                    properties.load(inputStream);
                }
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Could not load application.properties for docker bootstrap settings", exception);
            }

            boolean enabled = Boolean.parseBoolean(
                    properties.getProperty("app.startup.docker.enabled", "true"));

            String requiredPorts = properties.getProperty("app.startup.docker.required-ports", "5432,1025");
            List<Integer> ports = Arrays.stream(requiredPorts.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Integer::parseInt)
                    .toList();

            long connectTimeoutMillis = Long.parseLong(
                    properties.getProperty("app.startup.docker.connect-timeout-millis", "500"));
            long waitTimeoutSeconds = Long.parseLong(
                    properties.getProperty("app.startup.docker.wait-timeout-seconds", "60"));
            long pollIntervalMillis = Long.parseLong(
                    properties.getProperty("app.startup.docker.poll-interval-millis", "1000"));
            long composeCommandTimeoutSeconds = Long.parseLong(
                    properties.getProperty("app.startup.docker.compose-timeout-seconds", "120"));

            return new Settings(
                    enabled,
                    ports,
                    Duration.ofMillis(connectTimeoutMillis),
                    Duration.ofSeconds(waitTimeoutSeconds),
                    Duration.ofMillis(pollIntervalMillis),
                    Duration.ofSeconds(composeCommandTimeoutSeconds)
            );
        }
    }

    interface CommandExecutor {
        CommandResult execute(Path workingDirectory, List<String> command, Duration timeout)
                throws IOException, InterruptedException;
    }

    record CommandResult(int exitCode, String output) {
    }

    static class ProcessCommandExecutor implements CommandExecutor {

        @Override
        public CommandResult execute(Path workingDirectory, List<String> command, Duration timeout)
                throws IOException, InterruptedException {
            Process process = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Timeout after " + timeout.toSeconds() + "s while executing: "
                        + String.join(" ", command));
            }

            String output;
            try (InputStream inputStream = process.getInputStream()) {
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
            }

            return new CommandResult(process.exitValue(), output);
        }
    }

    interface PortProbe {
        boolean isOpen(String host, int port, Duration timeout);
    }

    static class SocketPortProbe implements PortProbe {

        @Override
        public boolean isOpen(String host, int port, Duration timeout) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
                return true;
            } catch (IOException exception) {
                return false;
            }
        }
    }
}

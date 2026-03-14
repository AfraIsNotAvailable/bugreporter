package com.group11.bugreporter.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DockerComposeBootstrapTest {

    @Test
    void doesNotRunComposeWhenDependenciesAreAlreadyUp() {
        AtomicInteger commandsExecuted = new AtomicInteger();
        DockerComposeBootstrap bootstrap = new DockerComposeBootstrap(
                testSettings(),
                (workingDirectory, command, timeout) -> {
                    commandsExecuted.incrementAndGet();
                    return new DockerComposeBootstrap.CommandResult(0, "ok");
                },
                (host, port, timeout) -> true);

        assertDoesNotThrow(bootstrap::ensureReady);
        assertEquals(0, commandsExecuted.get());
    }

    @Test
    void runsComposeWhenDependenciesAreDownAndEventuallyUp() {
        AtomicInteger commandsExecuted = new AtomicInteger();
        AtomicBoolean isUp = new AtomicBoolean(false);

        DockerComposeBootstrap bootstrap = new DockerComposeBootstrap(
                testSettings(),
                (workingDirectory, command, timeout) -> {
                    commandsExecuted.incrementAndGet();
                    isUp.set(true);
                    return new DockerComposeBootstrap.CommandResult(0, "started");
                },
                (host, port, timeout) -> isUp.get());

        assertDoesNotThrow(bootstrap::ensureReady);
        assertEquals(1, commandsExecuted.get());
    }

    @Test
    void failsWhenComposeCommandCannotBeExecuted() {
        DockerComposeBootstrap bootstrap = new DockerComposeBootstrap(
                testSettings(),
                (workingDirectory, command, timeout) -> {
                    throw new java.io.IOException("docker not found");
                },
                (host, port, timeout) -> false);

        assertThrows(IllegalStateException.class, bootstrap::ensureReady);
    }

    private static DockerComposeBootstrap.Settings testSettings() {
        return new DockerComposeBootstrap.Settings(
                true,
                List.of(5432),
                Duration.ofMillis(10),
                Duration.ofMillis(100),
                Duration.ofMillis(5),
                Duration.ofSeconds(1));
    }
}

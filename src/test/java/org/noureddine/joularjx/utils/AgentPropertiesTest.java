package org.noureddine.joularjx.utils;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AgentPropertiesTest {

    @Test
    @ExpectSystemExitWithStatus(1)
    void loadNonExistentFile() throws IOException {
        try (final FileSystem fs = MemoryFileSystemBuilder.newEmpty().build()) {
            new AgentProperties(fs);
        }
    }

    @Test
    void loadEmptyFile() throws IOException {
        try (final FileSystem fs = MemoryFileSystemBuilder.newEmpty().build()) {
            Files.createFile(fs.getPath("config.properties"));

            AgentProperties properties = new AgentProperties(fs);

            assertAll(
                    () -> assertFalse(properties.filtersMethod("")),
                    () -> assertNull(properties.getPowerMonitorPath()),
                    () -> assertFalse(properties.overwritesRuntimeData()),
                    () -> assertFalse(properties.savesRuntimeData()),
                    () -> assertEquals(Level.INFO, properties.getLoggerLevel()),
                    () -> assertFalse(properties.loadConsumptionEvolution()),
                    () -> assertEquals("evolution", properties.loadEvolutionDataPath()),
                    () -> assertFalse(properties.loadAgentConsumption())
            );
        }
    }

    @Test
    void fullConfiguration() throws IOException {
        try (final FileSystem fs = MemoryFileSystemBuilder.newEmpty().build()) {
            String path = "custom/path";
            String props = "filter-method-names=org.noureddine.joularjx\n" +
                                "powermonitor-path=C:\\\\joularjx\\\\PowerMonitor.exe\n" +
                                "save-runtime-data=true\n"+
                                "overwrite-runtime-data=true\n"+
                                "track-consumption-evolution=true\n"+
                                "evolution-data-path="+path+"\n"+
                                "hide-agent-consumption=true";
            Files.write(fs.getPath("config.properties"), (props).getBytes(StandardCharsets.UTF_8));

            AgentProperties properties = new AgentProperties(fs);

            assertAll(
                    () -> assertTrue(properties.filtersMethod("org.noureddine.joularjx")),
                    () -> assertEquals("C:\\joularjx\\PowerMonitor.exe", properties.getPowerMonitorPath()),
                    () -> assertTrue(properties.savesRuntimeData()),
                    () -> assertTrue(properties.overwritesRuntimeData()),
                    () -> assertTrue(properties.trackConsumptionEvolution()),
                    () -> assertEquals(path, properties.getEvolutionDataPath()),
                    () -> assertTrue(properties.hideAgentConsumption())
            );
        }
    }

    @Test
    void multipleFilterMethods() throws IOException {
        try (final FileSystem fs = MemoryFileSystemBuilder.newEmpty().build()) {
            Files.write(fs.getPath("config.properties"),
                    "filter-method-names=org.noureddine.joularjx,org.noureddine.joularjx2".getBytes(StandardCharsets.UTF_8));

            AgentProperties properties = new AgentProperties(fs);

            assertAll(
                    () -> assertTrue(properties.filtersMethod("org.noureddine.joularjx")),
                    () -> assertTrue(properties.filtersMethod("org.noureddine.joularjx2")),
                    () -> assertNull(properties.getPowerMonitorPath())
            );
        }
    }

    @Test
    void capsBoolean() throws IOException {
        try (final FileSystem fs = MemoryFileSystemBuilder.newEmpty().build()) {
            Files.write(fs.getPath("config.properties"),
                    "save-runtime-data=TrUe\noverwrite-runtime-data=FaLse".getBytes(StandardCharsets.UTF_8));

            AgentProperties properties = new AgentProperties(fs);

            assertAll(
                    () -> assertTrue(properties.savesRuntimeData()),
                    () -> assertFalse(properties.overwritesRuntimeData())
            );
        }
    }

    static Stream<Arguments> getLogLevels() {
        return Stream.of(
                Arguments.of("INFO", Level.INFO),
                Arguments.of("OFF", Level.OFF),
                Arguments.of("SEVERE", Level.SEVERE),
                Arguments.of("WARNING", Level.WARNING),
                Arguments.of("FINE", Level.FINE),
                Arguments.of("CONFIG", Level.CONFIG),
                Arguments.of("ALL", Level.ALL),
                Arguments.of("FINER", Level.FINER),
                Arguments.of("FINEST", Level.FINEST)
        );
    }

    @ParameterizedTest
    @MethodSource("getLogLevels")
    void logLevel(final String level, final Level expected) throws IOException {
        try (final FileSystem fs = MemoryFileSystemBuilder.newEmpty().build()) {
            Files.write(fs.getPath("config.properties"),
                    ("logger-level=" + level).getBytes(StandardCharsets.UTF_8));

            AgentProperties properties = new AgentProperties(fs);

            assertEquals(expected, properties.getLoggerLevel());
        }
    }
}
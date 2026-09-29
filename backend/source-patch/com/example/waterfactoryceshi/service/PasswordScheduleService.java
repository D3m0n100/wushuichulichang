package com.example.waterfactoryceshi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class PasswordScheduleService {

    private static final Logger log = LoggerFactory.getLogger(PasswordScheduleService.class);
    private static final String STARTED_AT_KEY = "startedAtEpochMillis";
    private static final String HIGHEST_STAGE_KEY = "highestStage";

    @Value("${auth.local.initial-password}")
    private String initialPassword;

    @Value("${auth.local.password}")
    private String stageOnePassword;

    @Value("${auth.local.password-2}")
    private String stageTwoPassword;

    @Value("${auth.local.password-3}")
    private String stageThreePassword;

    @Value("${auth.local.password-4}")
    private String stageFourPassword;

    @Value("${auth.local.permanent-password}")
    private String permanentPassword;

    @Value("${auth.local.rotation-days:31}")
    private int rotationDays;

    @Value("${auth.local.initial-password-minutes:150}")
    private int initialPasswordMinutes;

    @Value("${auth.local.schedule-state-file:}")
    private String configuredStateFile;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private State state;
    private Path stateFile;
    private Path stateBackupFile;

    @PostConstruct
    public synchronized void initialize() {
        if (rotationDays <= 0) {
            throw new IllegalStateException("auth.local.rotation-days must be greater than zero");
        }
        if (initialPasswordMinutes <= 0) {
            throw new IllegalStateException("auth.local.initial-password-minutes must be greater than zero");
        }
        if (TimeUnit.MINUTES.toMillis(initialPasswordMinutes) >= TimeUnit.DAYS.toMillis(rotationDays)) {
            throw new IllegalStateException("auth.local.initial-password-minutes must be shorter than the rotation period");
        }
        validatePasswordHash("auth.local.initial-password", initialPassword);
        validatePasswordHash("auth.local.password", stageOnePassword);
        validatePasswordHash("auth.local.password-2", stageTwoPassword);
        validatePasswordHash("auth.local.password-3", stageThreePassword);
        validatePasswordHash("auth.local.password-4", stageFourPassword);
        validatePasswordHash("auth.local.permanent-password", permanentPassword);
        stateFile = resolveStateFile();
        stateBackupFile = stateFile.resolveSibling(stateFile.getFileName() + ".bak");
        state = loadOrCreateState();
        Snapshot snapshot = currentSnapshotLocked(System.currentTimeMillis());
        log.info("Password schedule initialized. stage={}, startedAt={}, stateFile={}",
                snapshot.getStage(), state.startedAtEpochMillis, stateFile);
    }

    public synchronized Snapshot authenticate(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }
        Snapshot snapshot = currentSnapshotLocked(System.currentTimeMillis());
        String scheduledPassword = passwordForStage(snapshot.getStage());
        if (matchesConfiguredPassword(rawPassword, scheduledPassword)
                || matchesConfiguredPassword(rawPassword, permanentPassword)) {
            return snapshot;
        }
        return null;
    }

    public synchronized Snapshot currentSnapshot() {
        return currentSnapshotLocked(System.currentTimeMillis());
    }

    public synchronized int getCurrentStage() {
        return currentSnapshotLocked(System.currentTimeMillis()).getStage();
    }

    private Snapshot currentSnapshotLocked(long nowEpochMillis) {
        if (state == null) {
            stateFile = resolveStateFile();
            state = loadOrCreateState();
        }

        int calculatedStage = calculateStage(state.startedAtEpochMillis, nowEpochMillis,
                rotationDays, initialPasswordMinutes);
        int effectiveStage = Math.max(state.highestStage, calculatedStage);
        if (effectiveStage != state.highestStage) {
            state = new State(state.startedAtEpochMillis, effectiveStage);
            persistState(state);
            log.info("Password schedule advanced to stage {}", effectiveStage);
        }

        long nextChangeAt = calculateNextChangeAt(state.startedAtEpochMillis, effectiveStage,
                rotationDays, initialPasswordMinutes);
        return new Snapshot(effectiveStage, nextChangeAt);
    }

    public static int calculateStage(long startedAtEpochMillis, long nowEpochMillis, int rotationDays) {
        return calculateStage(startedAtEpochMillis, nowEpochMillis, rotationDays, 150);
    }

    public static int calculateStage(long startedAtEpochMillis, long nowEpochMillis,
                                     int rotationDays, int initialPasswordMinutes) {
        if (rotationDays <= 0) {
            throw new IllegalArgumentException("rotationDays must be greater than zero");
        }
        if (initialPasswordMinutes <= 0) {
            throw new IllegalArgumentException("initialPasswordMinutes must be greater than zero");
        }
        long initialPeriodMillis = TimeUnit.MINUTES.toMillis(initialPasswordMinutes);
        long periodMillis = TimeUnit.DAYS.toMillis(rotationDays);
        if (initialPeriodMillis >= periodMillis) {
            throw new IllegalArgumentException("initialPasswordMinutes must be shorter than rotationDays");
        }
        long elapsed = Math.max(0L, nowEpochMillis - startedAtEpochMillis);
        if (elapsed < initialPeriodMillis) return 0;
        if (elapsed < periodMillis) return 1;
        if (elapsed < periodMillis * 2L) return 2;
        if (elapsed < periodMillis * 3L) return 3;
        return 4;
    }

    public static long calculateNextChangeAt(long startedAtEpochMillis, int stage, int rotationDays) {
        return calculateNextChangeAt(startedAtEpochMillis, stage, rotationDays, 150);
    }

    public static long calculateNextChangeAt(long startedAtEpochMillis, int stage,
                                             int rotationDays, int initialPasswordMinutes) {
        if (rotationDays <= 0) {
            throw new IllegalArgumentException("rotationDays must be greater than zero");
        }
        if (initialPasswordMinutes <= 0) {
            throw new IllegalArgumentException("initialPasswordMinutes must be greater than zero");
        }
        if (TimeUnit.MINUTES.toMillis(initialPasswordMinutes) >= TimeUnit.DAYS.toMillis(rotationDays)) {
            throw new IllegalArgumentException("initialPasswordMinutes must be shorter than rotationDays");
        }
        if (stage <= 0) {
            return startedAtEpochMillis + TimeUnit.MINUTES.toMillis(initialPasswordMinutes);
        }
        if (stage >= 4) return 0L;
        long periodMillis = TimeUnit.DAYS.toMillis(rotationDays);
        return startedAtEpochMillis + periodMillis * stage;
    }

    private String passwordForStage(int stage) {
        switch (stage) {
            case 0: return initialPassword;
            case 1: return stageOnePassword;
            case 2: return stageTwoPassword;
            case 3: return stageThreePassword;
            default: return stageFourPassword;
        }
    }

    private boolean matchesConfiguredPassword(String rawPassword, String configuredPassword) {
        return passwordEncoder.matches(rawPassword, configuredPassword);
    }

    private void validatePasswordHash(String propertyName, String configuredPassword) {
        if (configuredPassword == null
                || !(configuredPassword.startsWith("$2a$")
                || configuredPassword.startsWith("$2b$")
                || configuredPassword.startsWith("$2y$"))) {
            throw new IllegalStateException(propertyName + " must contain a BCrypt hash");
        }
    }

    private Path resolveStateFile() {
        String configured = configuredStateFile == null ? "" : configuredStateFile.trim();
        if (!configured.isEmpty()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        String programData = System.getenv("ProgramData");
        if (programData != null && !programData.trim().isEmpty()) {
            return Paths.get(programData, "JinanWaterBackend", "auth-password-schedule.properties")
                    .toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir"), "runtime", "auth-password-schedule.properties")
                .toAbsolutePath().normalize();
    }

    private State loadOrCreateState() {
        if (Files.exists(stateFile)) {
            try {
                State loaded = readState(stateFile);
                ensureBackupMatches(loaded);
                return loaded;
            } catch (RuntimeException primaryError) {
                if (!Files.exists(stateBackupFile)) throw primaryError;
                try {
                    State recovered = readState(stateBackupFile);
                    writeStateAtomically(stateFile, recovered);
                    log.warn("Recovered invalid password schedule state from backup: {}", stateBackupFile);
                    return recovered;
                } catch (RuntimeException backupError) {
                    primaryError.addSuppressed(backupError);
                    throw primaryError;
                }
            }
        }

        if (Files.exists(stateBackupFile)) {
            State recovered = readState(stateBackupFile);
            writeStateAtomically(stateFile, recovered);
            log.warn("Recovered missing password schedule state from backup: {}", stateBackupFile);
            return recovered;
        }

        State created = new State(System.currentTimeMillis(), 0);
        persistState(created);
        return created;
    }

    private State readState(Path sourceFile) {
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
            long startedAt = Long.parseLong(requiredProperty(properties, STARTED_AT_KEY));
            int highestStage = Integer.parseInt(requiredProperty(properties, HIGHEST_STAGE_KEY));
            if (startedAt <= 0L || highestStage < 0 || highestStage > 4) {
                throw new IllegalStateException("Password schedule state contains invalid values");
            }
            return new State(startedAt, highestStage);
        } catch (IOException | NumberFormatException e) {
            throw new IllegalStateException("Unable to read password schedule state: " + sourceFile, e);
        }
    }

    private void ensureBackupMatches(State loaded) {
        boolean backupMatches = false;
        if (Files.exists(stateBackupFile)) {
            try {
                State backup = readState(stateBackupFile);
                backupMatches = backup.startedAtEpochMillis == loaded.startedAtEpochMillis
                        && backup.highestStage == loaded.highestStage;
            } catch (RuntimeException ignored) {
                backupMatches = false;
            }
        }
        if (!backupMatches) {
            try {
                writeStateAtomically(stateBackupFile, loaded);
            } catch (RuntimeException e) {
                log.warn("Unable to refresh password schedule backup: {}", stateBackupFile, e);
            }
        }
    }

    private String requiredProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Password schedule state is missing " + key);
        }
        return value.trim();
    }

    private void persistState(State value) {
        writeStateAtomically(stateFile, value);
        try {
            writeStateAtomically(stateBackupFile, value);
        } catch (RuntimeException e) {
            log.warn("Unable to persist password schedule backup: {}", stateBackupFile, e);
        }
    }

    private void writeStateAtomically(Path targetFile, State value) {
        try {
            Path parent = targetFile.getParent();
            if (parent != null) Files.createDirectories(parent);

            Properties properties = new Properties();
            properties.setProperty(STARTED_AT_KEY, Long.toString(value.startedAtEpochMillis));
            properties.setProperty(HIGHEST_STAGE_KEY, Integer.toString(value.highestStage));

            Path temporaryFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                properties.store(writer, "Jinan wastewater authentication password schedule");
            }
            try {
                Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist password schedule state: " + targetFile, e);
        }
    }

    private static final class State {
        private final long startedAtEpochMillis;
        private final int highestStage;

        private State(long startedAtEpochMillis, int highestStage) {
            this.startedAtEpochMillis = startedAtEpochMillis;
            this.highestStage = highestStage;
        }
    }

    public static final class Snapshot {
        private final int stage;
        private final long nextChangeAtEpochMillis;

        private Snapshot(int stage, long nextChangeAtEpochMillis) {
            this.stage = stage;
            this.nextChangeAtEpochMillis = nextChangeAtEpochMillis;
        }

        public int getStage() {
            return stage;
        }

        public long getNextChangeAtEpochMillis() {
            return nextChangeAtEpochMillis;
        }
    }
}

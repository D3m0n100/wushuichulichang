/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  javax.annotation.PostConstruct
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordScheduleService {
    private static final Logger log = LoggerFactory.getLogger(PasswordScheduleService.class);
    private static final String STARTED_AT_KEY = "startedAtEpochMillis";
    private static final String HIGHEST_STAGE_KEY = "highestStage";
    @Value(value="${auth.local.initial-password}")
    private String initialPassword;
    @Value(value="${auth.local.password}")
    private String stageOnePassword;
    @Value(value="${auth.local.password-2}")
    private String stageTwoPassword;
    @Value(value="${auth.local.password-3}")
    private String stageThreePassword;
    @Value(value="${auth.local.password-4}")
    private String stageFourPassword;
    @Value(value="${auth.local.permanent-password}")
    private String permanentPassword;
    @Value(value="${auth.local.rotation-days:31}")
    private int rotationDays;
    @Value(value="${auth.local.initial-password-minutes:10}")
    private int initialPasswordMinutes;
    @Value(value="${auth.local.schedule-state-file:}")
    private String configuredStateFile;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private State state;
    private Path stateFile;
    private Path stateBackupFile;

    @PostConstruct
    public synchronized void initialize() {
        if (this.rotationDays <= 0) {
            throw new IllegalStateException("auth.local.rotation-days must be greater than zero");
        }
        if (this.initialPasswordMinutes <= 0) {
            throw new IllegalStateException("auth.local.initial-password-minutes must be greater than zero");
        }
        if (TimeUnit.MINUTES.toMillis(this.initialPasswordMinutes) >= TimeUnit.DAYS.toMillis(this.rotationDays)) {
            throw new IllegalStateException("auth.local.initial-password-minutes must be shorter than the rotation period");
        }
        this.validatePasswordHash("auth.local.initial-password", this.initialPassword);
        this.validatePasswordHash("auth.local.password", this.stageOnePassword);
        this.validatePasswordHash("auth.local.password-2", this.stageTwoPassword);
        this.validatePasswordHash("auth.local.password-3", this.stageThreePassword);
        this.validatePasswordHash("auth.local.password-4", this.stageFourPassword);
        this.validatePasswordHash("auth.local.permanent-password", this.permanentPassword);
        this.stateFile = this.resolveStateFile();
        this.stateBackupFile = this.stateFile.resolveSibling(this.stateFile.getFileName() + ".bak");
        this.state = this.loadOrCreateState();
        Snapshot snapshot = this.currentSnapshotLocked(System.currentTimeMillis());
        log.info("Password schedule initialized. stage={}, startedAt={}, stateFile={}", new Object[]{snapshot.getStage(), this.state.startedAtEpochMillis, this.stateFile});
    }

    public synchronized Snapshot authenticate(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Snapshot snapshot = this.currentSnapshotLocked(System.currentTimeMillis());
        String string2 = this.passwordForStage(snapshot.getStage());
        if (this.matchesConfiguredPassword(string, string2) || this.matchesConfiguredPassword(string, this.permanentPassword)) {
            return snapshot;
        }
        return null;
    }

    public synchronized Snapshot currentSnapshot() {
        return this.currentSnapshotLocked(System.currentTimeMillis());
    }

    public synchronized int getCurrentStage() {
        return this.currentSnapshotLocked(System.currentTimeMillis()).getStage();
    }

    private Snapshot currentSnapshotLocked(long l) {
        if (this.state == null) {
            this.stateFile = this.resolveStateFile();
            this.state = this.loadOrCreateState();
        }
        int n = PasswordScheduleService.calculateStage(this.state.startedAtEpochMillis, l, this.rotationDays, this.initialPasswordMinutes);
        int n2 = Math.max(this.state.highestStage, n);
        if (n2 != this.state.highestStage) {
            this.state = new State(this.state.startedAtEpochMillis, n2);
            this.persistState(this.state);
            log.info("Password schedule advanced to stage {}", (Object)n2);
        }
        long l2 = PasswordScheduleService.calculateNextChangeAt(this.state.startedAtEpochMillis, n2, this.rotationDays, this.initialPasswordMinutes);
        return new Snapshot(n2, l2);
    }

    public static int calculateStage(long l, long l2, int n) {
        return PasswordScheduleService.calculateStage(l, l2, n, 10);
    }

    public static int calculateStage(long l, long l2, int n, int n2) {
        long l3;
        if (n <= 0) {
            throw new IllegalArgumentException("rotationDays must be greater than zero");
        }
        if (n2 <= 0) {
            throw new IllegalArgumentException("initialPasswordMinutes must be greater than zero");
        }
        long l4 = TimeUnit.MINUTES.toMillis(n2);
        if (l4 >= (l3 = TimeUnit.DAYS.toMillis(n))) {
            throw new IllegalArgumentException("initialPasswordMinutes must be shorter than rotationDays");
        }
        long l5 = Math.max(0L, l2 - l);
        if (l5 < l4) {
            return 0;
        }
        if (l5 < l3) {
            return 1;
        }
        if (l5 < l3 * 2L) {
            return 2;
        }
        if (l5 < l3 * 3L) {
            return 3;
        }
        return 4;
    }

    public static long calculateNextChangeAt(long l, int n, int n2) {
        return PasswordScheduleService.calculateNextChangeAt(l, n, n2, 10);
    }

    public static long calculateNextChangeAt(long l, int n, int n2, int n3) {
        if (n2 <= 0) {
            throw new IllegalArgumentException("rotationDays must be greater than zero");
        }
        if (n3 <= 0) {
            throw new IllegalArgumentException("initialPasswordMinutes must be greater than zero");
        }
        if (TimeUnit.MINUTES.toMillis(n3) >= TimeUnit.DAYS.toMillis(n2)) {
            throw new IllegalArgumentException("initialPasswordMinutes must be shorter than rotationDays");
        }
        if (n <= 0) {
            return l + TimeUnit.MINUTES.toMillis(n3);
        }
        if (n >= 4) {
            return 0L;
        }
        long l2 = TimeUnit.DAYS.toMillis(n2);
        return l + l2 * (long)n;
    }

    private String passwordForStage(int n) {
        switch (n) {
            case 0: {
                return this.initialPassword;
            }
            case 1: {
                return this.stageOnePassword;
            }
            case 2: {
                return this.stageTwoPassword;
            }
            case 3: {
                return this.stageThreePassword;
            }
        }
        return this.stageFourPassword;
    }

    private boolean matchesConfiguredPassword(String string, String string2) {
        return this.passwordEncoder.matches((CharSequence)string, string2);
    }

    private void validatePasswordHash(String string, String string2) {
        if (string2 == null || !string2.startsWith("$2a$") && !string2.startsWith("$2b$") && !string2.startsWith("$2y$")) {
            throw new IllegalStateException(string + " must contain a BCrypt hash");
        }
    }

    private Path resolveStateFile() {
        String string;
        String string2 = string = this.configuredStateFile == null ? "" : this.configuredStateFile.trim();
        if (!string.isEmpty()) {
            return Paths.get(string, new String[0]).toAbsolutePath().normalize();
        }
        String string3 = System.getenv("ProgramData");
        if (string3 != null && !string3.trim().isEmpty()) {
            return Paths.get(string3, "JinanWaterBackend", "auth-password-schedule.properties").toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir"), "runtime", "auth-password-schedule.properties").toAbsolutePath().normalize();
    }

    private State loadOrCreateState() {
        if (Files.exists(this.stateFile, new LinkOption[0])) {
            try {
                State state = this.readState(this.stateFile);
                this.ensureBackupMatches(state);
                return state;
            }
            catch (RuntimeException runtimeException) {
                if (!Files.exists(this.stateBackupFile, new LinkOption[0])) {
                    throw runtimeException;
                }
                try {
                    State state = this.readState(this.stateBackupFile);
                    this.writeStateAtomically(this.stateFile, state);
                    log.warn("Recovered invalid password schedule state from backup: {}", (Object)this.stateBackupFile);
                    return state;
                }
                catch (RuntimeException runtimeException2) {
                    runtimeException.addSuppressed(runtimeException2);
                    throw runtimeException;
                }
            }
        }
        if (Files.exists(this.stateBackupFile, new LinkOption[0])) {
            State state = this.readState(this.stateBackupFile);
            this.writeStateAtomically(this.stateFile, state);
            log.warn("Recovered missing password schedule state from backup: {}", (Object)this.stateBackupFile);
            return state;
        }
        State state = new State(System.currentTimeMillis(), 0);
        this.persistState(state);
        return state;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private State readState(Path path) {
        Properties properties = new Properties();
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            properties.load(bufferedReader);
            long l = Long.parseLong(this.requiredProperty(properties, STARTED_AT_KEY));
            int n = Integer.parseInt(this.requiredProperty(properties, HIGHEST_STAGE_KEY));
            if (l <= 0L) throw new IllegalStateException("Password schedule state contains invalid values");
            if (n < 0) throw new IllegalStateException("Password schedule state contains invalid values");
            if (n > 4) {
                throw new IllegalStateException("Password schedule state contains invalid values");
            }
            State state = new State(l, n);
            return state;
        }
        catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Unable to read password schedule state: " + path, exception);
        }
    }

    private void ensureBackupMatches(State state) {
        boolean bl = false;
        if (Files.exists(this.stateBackupFile, new LinkOption[0])) {
            try {
                State state2 = this.readState(this.stateBackupFile);
                bl = state2.startedAtEpochMillis == state.startedAtEpochMillis && state2.highestStage == state.highestStage;
            }
            catch (RuntimeException runtimeException) {
                bl = false;
            }
        }
        if (!bl) {
            try {
                this.writeStateAtomically(this.stateBackupFile, state);
            }
            catch (RuntimeException runtimeException) {
                log.warn("Unable to refresh password schedule backup: {}", (Object)this.stateBackupFile, (Object)runtimeException);
            }
        }
    }

    private String requiredProperty(Properties properties, String string) {
        String string2 = properties.getProperty(string);
        if (string2 == null || string2.trim().isEmpty()) {
            throw new IllegalStateException("Password schedule state is missing " + string);
        }
        return string2.trim();
    }

    private void persistState(State state) {
        this.writeStateAtomically(this.stateFile, state);
        try {
            this.writeStateAtomically(this.stateBackupFile, state);
        }
        catch (RuntimeException runtimeException) {
            log.warn("Unable to persist password schedule backup: {}", (Object)this.stateBackupFile, (Object)runtimeException);
        }
    }

    private void writeStateAtomically(Path path, State state) {
        try {
            Path path2 = path.getParent();
            if (path2 != null) {
                Files.createDirectories(path2, new FileAttribute[0]);
            }
            Properties properties = new Properties();
            properties.setProperty(STARTED_AT_KEY, Long.toString(state.startedAtEpochMillis));
            properties.setProperty(HIGHEST_STAGE_KEY, Integer.toString(state.highestStage));
            Path path3 = path.resolveSibling(path.getFileName() + ".tmp");
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path3, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);){
                properties.store(bufferedWriter, "Jinan wastewater authentication password schedule");
            }
            try {
                Files.move(path3, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                Files.move(path3, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Unable to persist password schedule state: " + path, iOException);
        }
    }

    public static final class Snapshot {
        private final int stage;
        private final long nextChangeAtEpochMillis;

        private Snapshot(int n, long l) {
            this.stage = n;
            this.nextChangeAtEpochMillis = l;
        }

        public int getStage() {
            return this.stage;
        }

        public long getNextChangeAtEpochMillis() {
            return this.nextChangeAtEpochMillis;
        }
    }

    private static final class State {
        private final long startedAtEpochMillis;
        private final int highestStage;

        private State(long l, int n) {
            this.startedAtEpochMillis = l;
            this.highestStage = n;
        }
    }
}

package com.example.waterfactoryceshi.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Arrays;

@Component
public class JwtUtil {

    public static final int AUTH_VERSION = 3;

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String ISSUER = "jinan-water-backend";
    private static final int SECRET_BYTES = 64;

    private final Key signingKey;

    public JwtUtil(@Value("${auth.jwt.secret-file:}") String configuredSecretFile) {
        Path secretFile = resolveSecretFile(configuredSecretFile);
        this.signingKey = Keys.hmacShaKeyFor(loadOrCreateSecret(secretFile));
        log.info("JWT signing key initialized from {}", secretFile);
    }

    public String generateToken(String username, String role, int passwordStage,
                                long passwordValidUntilEpochMillis) {
        JwtBuilder builder = Jwts.builder()
                .setIssuer(ISSUER)
                .setSubject(username)
                .claim("role", role)
                .claim("authVersion", AUTH_VERSION)
                .claim("passwordStage", passwordStage)
                .setIssuedAt(new Date());

        if (passwordValidUntilEpochMillis > 0L) {
            builder.claim("passwordValidUntil", passwordValidUntilEpochMillis);
        }

        return builder.signWith(signingKey, SignatureAlgorithm.HS256).compact();
    }

    public int getAuthVersion() {
        return AUTH_VERSION;
    }

    public boolean validateToken(String token, int expectedPasswordStage) {
        try {
            Claims claims = parseClaims(token);
            Number authVersion = claims.get("authVersion", Number.class);
            Number tokenStage = claims.get("passwordStage", Number.class);
            if (authVersion == null || authVersion.intValue() != AUTH_VERSION
                    || tokenStage == null || tokenStage.intValue() != expectedPasswordStage
                    || claims.getSubject() == null || claims.getSubject().trim().isEmpty()) {
                return false;
            }

            if (expectedPasswordStage < 4) {
                Number validUntil = claims.get("passwordValidUntil", Number.class);
                return validUntil != null && System.currentTimeMillis() < validUntil.longValue();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            return parseClaims(token).get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(ISSUER)
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Path resolveSecretFile(String configuredSecretFile) {
        String configured = configuredSecretFile == null ? "" : configuredSecretFile.trim();
        if (!configured.isEmpty()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        String programData = System.getenv("ProgramData");
        if (programData != null && !programData.trim().isEmpty()) {
            return Paths.get(programData, "JinanWaterBackend", "jwt-signing.key")
                    .toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir"), "runtime", "jwt-signing.key")
                .toAbsolutePath().normalize();
    }

    private byte[] loadOrCreateSecret(Path secretFile) {
        Path backupFile = secretFile.resolveSibling(secretFile.getFileName() + ".bak");
        if (Files.exists(secretFile)) {
            try {
                byte[] secret = readSecret(secretFile);
                boolean backupMatches = false;
                if (Files.exists(backupFile)) {
                    try {
                        backupMatches = Arrays.equals(secret, readSecret(backupFile));
                    } catch (RuntimeException ignored) {
                        backupMatches = false;
                    }
                }
                if (!backupMatches) writeSecretAtomically(backupFile, secret);
                return secret;
            } catch (RuntimeException e) {
                if (!Files.exists(backupFile)) throw e;
                byte[] recovered = readSecret(backupFile);
                writeSecretAtomically(secretFile, recovered);
                log.warn("Recovered invalid JWT signing key file from backup: {}", backupFile);
                return recovered;
            }
        }

        if (Files.exists(backupFile)) {
            byte[] recovered = readSecret(backupFile);
            writeSecretAtomically(secretFile, recovered);
            log.warn("Recovered missing JWT signing key file from backup: {}", backupFile);
            return recovered;
        }

        byte[] created = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(created);
        writeSecretAtomically(secretFile, created);
        writeSecretAtomically(backupFile, created);
        return created;
    }

    private byte[] readSecret(Path secretFile) {
        try {
            String encoded = new String(Files.readAllBytes(secretFile), StandardCharsets.US_ASCII).trim();
            byte[] decoded = Base64.getDecoder().decode(encoded);
            if (decoded.length < 32) {
                throw new IllegalStateException("JWT signing key must contain at least 32 bytes");
            }
            return decoded;
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to read JWT signing key: " + secretFile, e);
        }
    }

    private void writeSecretAtomically(Path secretFile, byte[] secret) {
        try {
            Path parent = secretFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporaryFile = secretFile.resolveSibling(secretFile.getFileName() + ".tmp");
            Files.write(temporaryFile, Base64.getEncoder().encode(secret),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporaryFile, secretFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, secretFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist JWT signing key: " + secretFile, e);
        }
    }
}

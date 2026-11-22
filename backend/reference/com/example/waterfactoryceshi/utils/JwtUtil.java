/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  io.jsonwebtoken.Claims
 *  io.jsonwebtoken.JwtBuilder
 *  io.jsonwebtoken.Jwts
 *  io.jsonwebtoken.SignatureAlgorithm
 *  io.jsonwebtoken.security.Keys
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 */
package com.example.waterfactoryceshi.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    public static final int AUTH_VERSION = 3;
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String ISSUER = "jinan-water-backend";
    private static final int SECRET_BYTES = 64;
    private final Key signingKey;

    public JwtUtil(@Value(value="${auth.jwt.secret-file:}") String string) {
        Path path = this.resolveSecretFile(string);
        this.signingKey = Keys.hmacShaKeyFor((byte[])this.loadOrCreateSecret(path));
        log.info("JWT signing key initialized from {}", (Object)path);
    }

    public String generateToken(String string, String string2, int n, long l) {
        JwtBuilder jwtBuilder = Jwts.builder().setIssuer(ISSUER).setSubject(string).claim("role", (Object)string2).claim("authVersion", (Object)3).claim("passwordStage", (Object)n).setIssuedAt(new Date());
        if (l > 0L) {
            jwtBuilder.claim("passwordValidUntil", (Object)l);
        }
        return jwtBuilder.signWith(this.signingKey, SignatureAlgorithm.HS256).compact();
    }

    public int getAuthVersion() {
        return 3;
    }

    public boolean validateToken(String string, int n) {
        try {
            Claims claims = this.parseClaims(string);
            Number number = (Number)claims.get("authVersion", Number.class);
            Number number2 = (Number)claims.get("passwordStage", Number.class);
            if (number == null || number.intValue() != 3 || number2 == null || number2.intValue() != n || claims.getSubject() == null || claims.getSubject().trim().isEmpty()) {
                return false;
            }
            if (n < 4) {
                Number number3 = (Number)claims.get("passwordValidUntil", Number.class);
                return number3 != null && System.currentTimeMillis() < number3.longValue();
            }
            return true;
        }
        catch (Exception exception) {
            return false;
        }
    }

    public String getUsernameFromToken(String string) {
        try {
            return this.parseClaims(string).getSubject();
        }
        catch (Exception exception) {
            return null;
        }
    }

    public String getRoleFromToken(String string) {
        try {
            return (String)this.parseClaims(string).get("role", String.class);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private Claims parseClaims(String string) {
        return (Claims)Jwts.parserBuilder().requireIssuer(ISSUER).setSigningKey(this.signingKey).build().parseClaimsJws(string).getBody();
    }

    private Path resolveSecretFile(String string) {
        String string2;
        String string3 = string2 = string == null ? "" : string.trim();
        if (!string2.isEmpty()) {
            return Paths.get(string2, new String[0]).toAbsolutePath().normalize();
        }
        String string4 = System.getenv("ProgramData");
        if (string4 != null && !string4.trim().isEmpty()) {
            return Paths.get(string4, "JinanWaterBackend", "jwt-signing.key").toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir"), "runtime", "jwt-signing.key").toAbsolutePath().normalize();
    }

    private byte[] loadOrCreateSecret(Path path) {
        Path path2 = path.resolveSibling(path.getFileName() + ".bak");
        if (Files.exists(path, new LinkOption[0])) {
            try {
                byte[] byArray = this.readSecret(path);
                boolean bl = false;
                if (Files.exists(path2, new LinkOption[0])) {
                    try {
                        bl = Arrays.equals(byArray, this.readSecret(path2));
                    }
                    catch (RuntimeException runtimeException) {
                        bl = false;
                    }
                }
                if (!bl) {
                    this.writeSecretAtomically(path2, byArray);
                }
                return byArray;
            }
            catch (RuntimeException runtimeException) {
                if (!Files.exists(path2, new LinkOption[0])) {
                    throw runtimeException;
                }
                byte[] byArray = this.readSecret(path2);
                this.writeSecretAtomically(path, byArray);
                log.warn("Recovered invalid JWT signing key file from backup: {}", (Object)path2);
                return byArray;
            }
        }
        if (Files.exists(path2, new LinkOption[0])) {
            byte[] byArray = this.readSecret(path2);
            this.writeSecretAtomically(path, byArray);
            log.warn("Recovered missing JWT signing key file from backup: {}", (Object)path2);
            return byArray;
        }
        byte[] byArray = new byte[64];
        new SecureRandom().nextBytes(byArray);
        this.writeSecretAtomically(path, byArray);
        this.writeSecretAtomically(path2, byArray);
        return byArray;
    }

    private byte[] readSecret(Path path) {
        try {
            String string = new String(Files.readAllBytes(path), StandardCharsets.US_ASCII).trim();
            byte[] byArray = Base64.getDecoder().decode(string);
            if (byArray.length < 32) {
                throw new IllegalStateException("JWT signing key must contain at least 32 bytes");
            }
            return byArray;
        }
        catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to read JWT signing key: " + path, exception);
        }
    }

    private void writeSecretAtomically(Path path, byte[] byArray) {
        try {
            Path path2 = path.getParent();
            if (path2 != null) {
                Files.createDirectories(path2, new FileAttribute[0]);
            }
            Path path3 = path.resolveSibling(path.getFileName() + ".tmp");
            Files.write(path3, Base64.getEncoder().encode(byArray), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(path3, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                Files.move(path3, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException iOException) {
            throw new IllegalStateException("Unable to persist JWT signing key: " + path, iOException);
        }
    }
}

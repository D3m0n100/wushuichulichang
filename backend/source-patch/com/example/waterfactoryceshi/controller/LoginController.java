package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.service.PasswordScheduleService;
import com.example.waterfactoryceshi.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class LoginController {

    @Value("${auth.local.username:admin}")
    private String localUsername = "admin";

    @Value("${auth.local.role:admin}")
    private String localRole = "admin";

    private final PasswordScheduleService passwordScheduleService;
    private final JwtUtil jwtUtil;

    @Autowired
    public LoginController(PasswordScheduleService passwordScheduleService, JwtUtil jwtUtil) {
        this.passwordScheduleService = passwordScheduleService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData) {
        String username = trimToDefault(loginData.get("username"), localUsername);
        String password = loginData.get("password");
        if (password == null || password.isEmpty()) {
            return failure("password is required");
        }
        if (!localUsername.equals(username)) {
            return failure("user does not exist");
        }

        PasswordScheduleService.Snapshot snapshot = passwordScheduleService.authenticate(password);
        if (snapshot == null) {
            return failure("invalid password");
        }
        return success(localUsername, localRole, snapshot);
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        PasswordScheduleService.Snapshot snapshot = passwordScheduleService.currentSnapshot();
        String token = extractBearerToken(authorization);
        if (token == null || !jwtUtil.validateToken(token, snapshot.getStage())) {
            return ResponseEntity.status(401).body(failure("password changed or token invalid"));
        }
        String role = jwtUtil.getRoleFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        if (!localUsername.equals(username)) {
            return ResponseEntity.status(401).body(failure("token user invalid"));
        }
        if (!localRole.equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(failure("admin role required"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("username", username);
        result.put("role", role);
        result.put("authVersion", jwtUtil.getAuthVersion());
        result.put("passwordStage", snapshot.getStage());
        result.put("passwordValidUntil", snapshot.getNextChangeAtEpochMillis());
        result.put("serverTime", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private String trimToDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        return value.trim();
    }

    private Map<String, Object> success(String username, String role,
                                        PasswordScheduleService.Snapshot snapshot) {
        Map<String, Object> result = new HashMap<>();
        String token = jwtUtil.generateToken(username, role, snapshot.getStage(),
                snapshot.getNextChangeAtEpochMillis());
        result.put("success", true);
        result.put("message", "login successful");
        result.put("token", token);
        result.put("username", username);
        result.put("role", role);
        return result;
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}

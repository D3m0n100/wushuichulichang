/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.service.PasswordScheduleService;
import com.example.waterfactoryceshi.utils.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/auth", "/api/auth"})
public class LoginController {
    @Value(value="${auth.local.username:admin}")
    private String localUsername = "admin";
    @Value(value="${auth.local.role:admin}")
    private String localRole = "admin";
    private final PasswordScheduleService passwordScheduleService;
    private final JwtUtil jwtUtil;

    @Autowired
    public LoginController(PasswordScheduleService passwordScheduleService, JwtUtil jwtUtil) {
        this.passwordScheduleService = passwordScheduleService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(value={"/login"})
    public Map<String, Object> login(@RequestBody Map<String, String> map) {
        String string = this.trimToDefault(map.get("username"), this.localUsername);
        String string2 = map.get("password");
        if (string2 == null || string2.isEmpty()) {
            return this.failure("password is required");
        }
        if (!this.localUsername.equals(string)) {
            return this.failure("user does not exist");
        }
        PasswordScheduleService.Snapshot snapshot = this.passwordScheduleService.authenticate(string2);
        if (snapshot == null) {
            return this.failure("invalid password");
        }
        return this.success(this.localUsername, this.localRole, snapshot);
    }

    @GetMapping(value={"/session"})
    public ResponseEntity<Map<String, Object>> session(@RequestHeader(value="Authorization", required=false) String string) {
        PasswordScheduleService.Snapshot snapshot = this.passwordScheduleService.currentSnapshot();
        String string2 = this.extractBearerToken(string);
        if (string2 == null || !this.jwtUtil.validateToken(string2, snapshot.getStage())) {
            return ResponseEntity.status((int)401).body(this.failure("password changed or token invalid"));
        }
        String string3 = this.jwtUtil.getRoleFromToken(string2);
        String string4 = this.jwtUtil.getUsernameFromToken(string2);
        if (!this.localUsername.equals(string4)) {
            return ResponseEntity.status((int)401).body(this.failure("token user invalid"));
        }
        if (!this.localRole.equalsIgnoreCase(string3)) {
            return ResponseEntity.status((int)403).body(this.failure("admin role required"));
        }
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("success", true);
        hashMap.put("username", string4);
        hashMap.put("role", string3);
        hashMap.put("authVersion", this.jwtUtil.getAuthVersion());
        hashMap.put("passwordStage", snapshot.getStage());
        hashMap.put("passwordValidUntil", snapshot.getNextChangeAtEpochMillis());
        hashMap.put("serverTime", System.currentTimeMillis());
        return ResponseEntity.ok(hashMap);
    }

    private String extractBearerToken(String string) {
        if (string == null || !string.startsWith("Bearer ")) {
            return null;
        }
        String string2 = string.substring(7).trim();
        return string2.isEmpty() ? null : string2;
    }

    private String trimToDefault(String string, String string2) {
        if (string == null || string.trim().isEmpty()) {
            return string2;
        }
        return string.trim();
    }

    private Map<String, Object> success(String string, String string2, PasswordScheduleService.Snapshot snapshot) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        String string3 = this.jwtUtil.generateToken(string, string2, snapshot.getStage(), snapshot.getNextChangeAtEpochMillis());
        hashMap.put("success", true);
        hashMap.put("message", "login successful");
        hashMap.put("token", string3);
        hashMap.put("username", string);
        hashMap.put("role", string2);
        return hashMap;
    }

    private Map<String, Object> failure(String string) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("success", false);
        hashMap.put("message", string);
        return hashMap;
    }
}

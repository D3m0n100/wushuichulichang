/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 *  org.springframework.web.servlet.HandlerInterceptor
 */
package com.example.waterfactoryceshi.interceptor;

import com.example.waterfactoryceshi.service.PasswordScheduleService;
import com.example.waterfactoryceshi.utils.JwtUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor
implements HandlerInterceptor {
    private static final Set<String> WRITE_METHODS = new HashSet<String>(Arrays.asList("POST", "PUT", "PATCH", "DELETE"));
    private static final String[] EXCLUDE_PATHS = new String[]{"/auth/login", "/api/auth/login", "/auth/register", "/api/auth/register", "/error", "/favicon.ico"};
    private final PasswordScheduleService passwordScheduleService;
    private final JwtUtil jwtUtil;
    @Value(value="${auth.local.username:admin}")
    private String localUsername = "admin";

    @Autowired
    public JwtInterceptor(PasswordScheduleService passwordScheduleService, JwtUtil jwtUtil) {
        this.passwordScheduleService = passwordScheduleService;
        this.jwtUtil = jwtUtil;
    }

    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        int n;
        String string = httpServletRequest.getRequestURI();
        if (this.isExcludedPath(string) || this.isOptionsRequest(httpServletRequest) || !this.requiresAdminWriteAuth(httpServletRequest)) {
            return true;
        }
        String string2 = httpServletRequest.getHeader("Authorization");
        if (string2 == null || !string2.startsWith("Bearer ")) {
            this.writeJson(httpServletResponse, 401, "missing or invalid token");
            return false;
        }
        if (!this.jwtUtil.validateToken(string2 = string2.substring(7), n = this.passwordScheduleService.getCurrentStage())) {
            this.writeJson(httpServletResponse, 401, "password changed or token invalid");
            return false;
        }
        String string3 = this.jwtUtil.getRoleFromToken(string2);
        String string4 = this.jwtUtil.getUsernameFromToken(string2);
        if (!this.localUsername.equals(string4)) {
            this.writeJson(httpServletResponse, 401, "token user invalid");
            return false;
        }
        if (!"admin".equalsIgnoreCase(string3)) {
            this.writeJson(httpServletResponse, 403, "admin role required for PLC write");
            return false;
        }
        return true;
    }

    private boolean isExcludedPath(String string) {
        for (String string2 : EXCLUDE_PATHS) {
            if (!string.equals(string2) && !string.startsWith(string2)) continue;
            return true;
        }
        return false;
    }

    private boolean isOptionsRequest(HttpServletRequest httpServletRequest) {
        return "OPTIONS".equalsIgnoreCase(httpServletRequest.getMethod());
    }

    private boolean requiresAdminWriteAuth(HttpServletRequest httpServletRequest) {
        String string = httpServletRequest.getRequestURI();
        String string2 = httpServletRequest.getMethod();
        return string != null && string.startsWith("/api/plc/") && string2 != null && WRITE_METHODS.contains(string2.toUpperCase());
    }

    private void writeJson(HttpServletResponse httpServletResponse, int n, String string) throws Exception {
        httpServletResponse.setStatus(n);
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        httpServletResponse.getWriter().write("{\"success\":false,\"message\":\"" + string + "\"}");
    }
}

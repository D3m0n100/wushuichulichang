package com.example.waterfactoryceshi.interceptor;

import com.example.waterfactoryceshi.service.PasswordScheduleService;
import com.example.waterfactoryceshi.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_METHODS =
            new HashSet<>(Arrays.asList("POST", "PUT", "PATCH", "DELETE"));

    private static final String[] EXCLUDE_PATHS = {
            "/auth/login",
            "/api/auth/login",
            "/auth/register",
            "/api/auth/register",
            "/error",
            "/favicon.ico"
    };

    private final PasswordScheduleService passwordScheduleService;
    private final JwtUtil jwtUtil;

    @Value("${auth.local.username:admin}")
    private String localUsername = "admin";

    @Autowired
    public JwtInterceptor(PasswordScheduleService passwordScheduleService, JwtUtil jwtUtil) {
        this.passwordScheduleService = passwordScheduleService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        if (isExcludedPath(path) || isOptionsRequest(request) || !requiresAdminWriteAuth(request)) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            writeJson(response, 401, "missing or invalid token");
            return false;
        }

        token = token.substring(7);
        int currentPasswordStage = passwordScheduleService.getCurrentStage();
        if (!jwtUtil.validateToken(token, currentPasswordStage)) {
            writeJson(response, 401, "password changed or token invalid");
            return false;
        }

        String role = jwtUtil.getRoleFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        if (!localUsername.equals(username)) {
            writeJson(response, 401, "token user invalid");
            return false;
        }
        if (!"admin".equalsIgnoreCase(role)) {
            writeJson(response, 403, "admin role required for PLC write");
            return false;
        }
        return true;
    }

    private boolean isExcludedPath(String path) {
        for (String excludedPath : EXCLUDE_PATHS) {
            if (path.equals(excludedPath) || path.startsWith(excludedPath)) return true;
        }
        return false;
    }

    private boolean isOptionsRequest(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private boolean requiresAdminWriteAuth(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return path != null && path.startsWith("/api/plc/")
                && method != null && WRITE_METHODS.contains(method.toUpperCase());
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}

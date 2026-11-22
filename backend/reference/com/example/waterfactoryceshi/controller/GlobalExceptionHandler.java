/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.apache.catalina.connector.ClientAbortException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpStatus
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.ResponseStatus
 *  org.springframework.web.bind.annotation.RestControllerAdvice
 *  org.springframework.web.context.request.ServletWebRequest
 */
package com.example.waterfactoryceshi.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value={ClientAbortException.class})
    public void handleClientAbort(ClientAbortException exception) {
        log.debug("Client disconnected: {}", (Object)exception.getMessage());
    }

    @ExceptionHandler(value={Exception.class})
    @ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception exception, ServletWebRequest request) {
        Throwable root;
        for (root = exception; root.getCause() != null && root.getCause() != root; root = root.getCause()) {
        }
        LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", exception.getMessage());
        body.put("rootCause", root.getMessage());
        body.put("exception", exception.getClass().getName());
        body.put("path", request.getRequest().getRequestURI());
        return body;
    }
}

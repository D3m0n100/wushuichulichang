/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.service.B1PlcService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/plc/b1"})
public class B1PlcController {
    private final B1PlcService b1PlcService;

    public B1PlcController(B1PlcService b1PlcService) {
        this.b1PlcService = b1PlcService;
    }

    @GetMapping(value={"/overview"})
    public Map<String, Object> overview() {
        return this.success(this.b1PlcService.readOverview());
    }

    @GetMapping(value={"/catalog"})
    public Map<String, Object> catalog() {
        return this.success(this.b1PlcService.readOverview());
    }

    @GetMapping(value={"/devices/{deviceCode}"})
    public Map<String, Object> device(@PathVariable String deviceCode) {
        return this.success(this.b1PlcService.readDevice(deviceCode.toUpperCase()));
    }

    @PostMapping(value={"/devices/{deviceCode}/manual-mode"})
    public Map<String, Object> manualMode(@PathVariable String deviceCode, @RequestParam boolean enabled) {
        boolean written = this.b1PlcService.writeManualMode(deviceCode.toUpperCase(), enabled);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("enabled", enabled);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/devices/{deviceCode}/command"})
    public Map<String, Object> command(@PathVariable String deviceCode, @RequestParam(required=false) String action, @RequestParam(defaultValue="300") long pulseMs) {
        boolean written = this.b1PlcService.pulseCommand(deviceCode.toUpperCase(), action, pulseMs);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("action", action);
        data.put("pulseMs", pulseMs);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/parameters/{parameterCode}"})
    public Map<String, Object> parameter(@PathVariable String parameterCode, @RequestParam String value) {
        boolean written = this.b1PlcService.writeParameter(parameterCode.toUpperCase(), value);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("parameterCode", parameterCode.toUpperCase());
        data.put("value", value);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/instruments/{instrumentCode}"})
    public Map<String, Object> instrument(@PathVariable String instrumentCode, @RequestParam(required=false) Float upperLimit, @RequestParam(required=false) Float lowerLimit, @RequestParam(required=false) Float deviation) {
        boolean written = this.b1PlcService.writeInstrumentConfig(instrumentCode.toUpperCase(), upperLimit, lowerLimit, deviation);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("instrumentCode", instrumentCode.toUpperCase());
        data.put("upperLimit", upperLimit);
        data.put("lowerLimit", lowerLimit);
        data.put("deviation", deviation);
        data.put("written", written);
        return this.success(data);
    }

    private Map<String, Object> success(Object data) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }
}

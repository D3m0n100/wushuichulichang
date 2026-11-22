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

import com.example.waterfactoryceshi.service.M3PlcService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/plc/m3"})
public class M3PlcController {
    private final M3PlcService m3PlcService;

    public M3PlcController(M3PlcService m3PlcService) {
        this.m3PlcService = m3PlcService;
    }

    @GetMapping(value={"/overview"})
    public Map<String, Object> overview() {
        return this.success(this.m3PlcService.readOverview());
    }

    @GetMapping(value={"/catalog"})
    public Map<String, Object> catalog() {
        return this.success(this.m3PlcService.catalog());
    }

    @GetMapping(value={"/devices/{deviceCode}"})
    public Map<String, Object> device(@PathVariable String deviceCode) {
        return this.success(this.m3PlcService.readDevice(deviceCode.toUpperCase()));
    }

    @PostMapping(value={"/devices/{deviceCode}/manual-mode"})
    public Map<String, Object> manualMode(@PathVariable String deviceCode, @RequestParam boolean enabled) {
        boolean written = this.m3PlcService.writeManualMode(deviceCode.toUpperCase(), enabled);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("enabled", enabled);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/devices/{deviceCode}/command"})
    public Map<String, Object> command(@PathVariable String deviceCode, @RequestParam String action, @RequestParam(defaultValue="300") long pulseMs) {
        boolean written = this.m3PlcService.pulseCommand(deviceCode.toUpperCase(), action, pulseMs);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("action", action);
        data.put("pulseMs", pulseMs);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/devices/{deviceCode}/frequency"})
    public Map<String, Object> frequency(@PathVariable String deviceCode, @RequestParam float value) {
        boolean written = this.m3PlcService.writeFrequencySetpoint(deviceCode.toUpperCase(), value);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("value", Float.valueOf(value));
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/instruments/{instrumentCode}"})
    public Map<String, Object> instrument(@PathVariable String instrumentCode, @RequestParam(required=false) Float upperLimit, @RequestParam(required=false) Float lowerLimit, @RequestParam(required=false) Float deviation) {
        boolean written = this.m3PlcService.writeInstrumentConfig(instrumentCode.toUpperCase(), upperLimit, lowerLimit, deviation);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("instrumentCode", instrumentCode.toUpperCase());
        data.put("upperLimit", upperLimit);
        data.put("lowerLimit", lowerLimit);
        data.put("deviation", deviation);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/timers/{timerCode}"})
    public Map<String, Object> timer(@PathVariable String timerCode, @RequestParam(required=false) Integer runHours, @RequestParam(required=false) Integer runMinutes, @RequestParam(required=false) Integer runSeconds, @RequestParam(required=false) Integer stopHours, @RequestParam(required=false) Integer stopMinutes, @RequestParam(required=false) Integer stopSeconds) {
        boolean written = this.m3PlcService.writeTimerConfig(timerCode.toUpperCase(), runHours, runMinutes, runSeconds, stopHours, stopMinutes, stopSeconds);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("timerCode", timerCode.toUpperCase());
        data.put("runHours", runHours);
        data.put("runMinutes", runMinutes);
        data.put("runSeconds", runSeconds);
        data.put("stopHours", stopHours);
        data.put("stopMinutes", stopMinutes);
        data.put("stopSeconds", stopSeconds);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/logic-configs/{configCode}"})
    public Map<String, Object> logicConfig(@PathVariable String configCode, @RequestParam(required=false) Float startLevel, @RequestParam(required=false) Float stopLevel, @RequestParam(required=false) Integer delayMinutes, @RequestParam(required=false) Integer delaySeconds) {
        boolean written = this.m3PlcService.writeLogicConfig(configCode.toUpperCase(), startLevel, stopLevel, delayMinutes, delaySeconds);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("configCode", configCode.toUpperCase());
        data.put("startLevel", startLevel);
        data.put("stopLevel", stopLevel);
        data.put("delayMinutes", delayMinutes);
        data.put("delaySeconds", delaySeconds);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/parameters/{parameterCode}"})
    public Map<String, Object> parameter(@PathVariable String parameterCode, @RequestParam String value) {
        boolean written = this.m3PlcService.writeParameter(parameterCode.toUpperCase(), value);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("parameterCode", parameterCode.toUpperCase());
        data.put("value", value);
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

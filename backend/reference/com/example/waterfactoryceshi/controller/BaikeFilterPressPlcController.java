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

import com.example.waterfactoryceshi.controller.AbstractSmartPlcController;
import com.example.waterfactoryceshi.service.BaikeFilterPressPlcService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/plc/baike-filter-press"})
public class BaikeFilterPressPlcController
extends AbstractSmartPlcController {
    private final BaikeFilterPressPlcService baikeFilterPressPlcService;

    public BaikeFilterPressPlcController(BaikeFilterPressPlcService baikeFilterPressPlcService) {
        super(baikeFilterPressPlcService);
        this.baikeFilterPressPlcService = baikeFilterPressPlcService;
    }

    @GetMapping(value={"/overview"})
    public Map<String, Object> overview() {
        return this.success(this.baikeFilterPressPlcService.readOverview());
    }

    @GetMapping(value={"/devices/{deviceCode}"})
    public Map<String, Object> device(@PathVariable String deviceCode) {
        return this.success(this.baikeFilterPressPlcService.readDevice(deviceCode.toUpperCase()));
    }

    @PostMapping(value={"/devices/{deviceCode}/manual-mode"})
    public Map<String, Object> manualMode(@PathVariable String deviceCode, @RequestParam boolean enabled) {
        boolean written = this.baikeFilterPressPlcService.writeManualMode(deviceCode.toUpperCase(), enabled);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("enabled", enabled);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/devices/{deviceCode}/command"})
    public Map<String, Object> command(@PathVariable String deviceCode, @RequestParam(defaultValue="300") long pulseMs) {
        boolean written = this.baikeFilterPressPlcService.pulseCommand(deviceCode.toUpperCase(), pulseMs);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("pulseMs", pulseMs);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/parameters/{parameterCode}"})
    public Map<String, Object> parameter(@PathVariable String parameterCode, @RequestParam String value) {
        boolean written = this.baikeFilterPressPlcService.writeParameter(parameterCode.toUpperCase(), value);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("parameterCode", parameterCode.toUpperCase());
        data.put("value", value);
        data.put("written", written);
        return this.success(data);
    }
}

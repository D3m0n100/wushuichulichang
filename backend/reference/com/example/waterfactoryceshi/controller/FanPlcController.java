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
import com.example.waterfactoryceshi.service.FanPlcService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/plc/fan"})
public class FanPlcController
extends AbstractSmartPlcController {
    private final FanPlcService fanPlcService;

    public FanPlcController(FanPlcService fanPlcService) {
        super(fanPlcService);
        this.fanPlcService = fanPlcService;
    }

    @GetMapping(value={"/overview"})
    public Map<String, Object> overview() {
        return this.success(this.fanPlcService.readOverview());
    }

    @GetMapping(value={"/devices/{deviceCode}"})
    public Map<String, Object> device(@PathVariable String deviceCode) {
        return this.success(this.fanPlcService.readDevice(deviceCode.toUpperCase()));
    }

    @PostMapping(value={"/devices/{deviceCode}/command"})
    public Map<String, Object> command(@PathVariable String deviceCode, @RequestParam(defaultValue="HOLD_ON") String action, @RequestParam(defaultValue="300") long pulseMs) {
        boolean written = this.fanPlcService.executeCommand(deviceCode.toUpperCase(), action, pulseMs);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceCode", deviceCode.toUpperCase());
        data.put("action", action.toUpperCase());
        data.put("pulseMs", pulseMs);
        data.put("written", written);
        return this.success(data);
    }

    @PostMapping(value={"/parameters/{parameterCode}"})
    public Map<String, Object> parameter(@PathVariable String parameterCode, @RequestParam String value) {
        boolean written = this.fanPlcService.writeParameter(parameterCode.toUpperCase(), value);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("parameterCode", parameterCode.toUpperCase());
        data.put("value", value);
        data.put("written", written);
        return this.success(data);
    }
}

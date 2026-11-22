/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.web.bind.annotation.GetMapping
 */
package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.service.AbstractS7200SmartPlcService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

public abstract class AbstractSmartPlcController {
    protected final AbstractS7200SmartPlcService plcService;

    protected AbstractSmartPlcController(AbstractS7200SmartPlcService plcService) {
        this.plcService = plcService;
    }

    @GetMapping(value={"/catalog"})
    public Map<String, Object> catalog() {
        return this.success(this.plcService.catalog());
    }

    protected Map<String, Object> success(Object data) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }
}

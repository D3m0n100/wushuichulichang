/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.controller.GenericSmartPlcController;
import com.example.waterfactoryceshi.service.RotorPumpPlcService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/plc/rotor-pump"})
public class RotorPumpPlcController
extends GenericSmartPlcController {
    public RotorPumpPlcController(RotorPumpPlcService plcService) {
        super(plcService);
    }
}

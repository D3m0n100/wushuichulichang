/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.controller.GenericSmartPlcController;
import com.example.waterfactoryceshi.service.PamPlcService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/plc/pam"})
public class PamPlcController
extends GenericSmartPlcController {
    public PamPlcController(PamPlcService plcService) {
        super(plcService);
    }
}

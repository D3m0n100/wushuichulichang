/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.controller.GenericSmartPlcController;
import com.example.waterfactoryceshi.service.CoarseScreenPlcService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/plc/coarse-screen"})
public class CoarseScreenPlcController
extends GenericSmartPlcController {
    public CoarseScreenPlcController(CoarseScreenPlcService plcService) {
        super(plcService);
    }
}

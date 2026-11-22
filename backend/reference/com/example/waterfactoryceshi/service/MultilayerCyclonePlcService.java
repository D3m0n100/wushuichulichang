/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.MultilayerCyclonePlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MultilayerCyclonePlcService
extends AbstractWastewaterSmartPlcService {
    public MultilayerCyclonePlcService(MultilayerCyclonePlcConnectionPool connectionPool, @Value(value="${plc.multilayerCyclone.ip}") String plcIp, @Value(value="${plc.multilayerCyclone.rack:0}") int rack, @Value(value="${plc.multilayerCyclone.slot:1}") int slot) {
        super(connectionPool, "MultilayerCyclone", "S7-200 SMART SR30", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerInputStatusDevice("MLC_PUMP_1", "\u6da1\u6d41\u6392\u7802\u6cf51", "pump", 0, 0, 0, 1);
        this.registerInputStatusDevice("MLC_PUMP_2", "\u6da1\u6d41\u6392\u7802\u6cf52", "pump", 0, 2, 0, 3);
        this.registerInputStatusDevice("MLC_SEPARATOR", "\u5206\u7802\u7535\u673a", "motor", 0, 4, 0, 5);
        this.registerInputStatusDevice("MLC_WASH_11", "\u51b2\u6d17\u6cf51-1", "pump", 0, 6, 0, 7);
        this.registerInputStatusDevice("MLC_WASH_12", "\u51b2\u6d17\u6cf51-2", "pump", 1, 0, 1, 1);
        this.registerInputStatusDevice("MLC_WASH_21", "\u51b2\u6d17\u6cf52-1", "pump", 1, 2, 1, 3);
        this.registerInputStatusDevice("MLC_WASH_22", "\u51b2\u6d17\u6cf52-2", "pump", 1, 4, 1, 5);
        this.registerBoolInputParameter("MLC_LOCAL_AUTO", "\u5c31\u5730\u81ea\u52a8", 1, 6);
        this.registerBoolInputParameter("MLC_REMOTE_CONTROL", "\u8fdc\u7a0b\u63a7\u5236", 1, 7);
        this.registerBoolInputParameter("MLC_ESTOP", "\u6025\u505c", 2, 0);
        this.registerIntVParameter("MLC_SYSTEM1_WASH_TIME", "\u7cfb\u7edf1\u51b2\u6d17\u65f6\u95f4", 2000, false);
        this.registerIntVParameter("MLC_SYSTEM1_SWITCH_TIME", "\u7cfb\u7edf1\u51b2\u6d17\u6cf5\u5207\u6362\u65f6\u95f4", 2002, false);
        this.registerIntVParameter("MLC_PUMP1_RUN_TIME", "\u6da1\u6d41\u6392\u7802\u6cf51\u8fd0\u884c\u65f6\u95f4", 2004, false);
        this.registerIntVParameter("MLC_PUMP1_INTERVAL_TIME", "\u6da1\u6d41\u6392\u7802\u6cf51\u95f4\u6b47\u65f6\u95f4", 2006, false);
        this.registerIntVParameter("MLC_SYSTEM2_WASH_TIME", "\u7cfb\u7edf2\u51b2\u6d17\u65f6\u95f4", 2008, false);
        this.registerIntVParameter("MLC_SYSTEM2_SWITCH_TIME", "\u7cfb\u7edf2\u51b2\u6d17\u6cf5\u5207\u6362\u65f6\u95f4", 2010, false);
        this.registerIntVParameter("MLC_PUMP2_RUN_TIME", "\u6da1\u6d41\u6392\u7802\u6cf52\u8fd0\u884c\u65f6\u95f4", 2012, false);
        this.registerIntVParameter("MLC_PUMP2_INTERVAL_TIME", "\u6da1\u6d41\u6392\u7802\u6cf52\u95f4\u6b47\u65f6\u95f4", 2014, false);
        this.registerIntVParameter("MLC_COARSE_INTERVAL_TIME", "\u7c97\u683c\u6805\u95f4\u6b47\u65f6\u95f4", 2016, false);
        this.registerIntVParameter("MLC_CONVEYOR_DELAY_TIME", "\u8f93\u9001\u673a\u5ef6\u65f6\u65f6\u95f4", 2018, false);
        this.registerBoolVParameter("MLC_REMOTE_AUTO_START", "\u8fdc\u7a0b\u81ea\u52a8\u542f\u52a8", 50, 0, false);
        this.registerBoolVParameter("MLC_AUTO_1", "1#\u81ea\u52a8", 0, 1, false);
        this.registerBoolVParameter("MLC_AUTO_2", "2#\u81ea\u52a8", 0, 2, false);
    }
}

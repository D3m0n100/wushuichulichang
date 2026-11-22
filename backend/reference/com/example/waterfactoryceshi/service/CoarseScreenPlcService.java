/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.CoarseScreenPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CoarseScreenPlcService
extends AbstractWastewaterSmartPlcService {
    private static final long DEFAULT_PULSE_MS = 300L;

    public CoarseScreenPlcService(CoarseScreenPlcConnectionPool connectionPool, @Value(value="${plc.coarseScreen.ip}") String plcIp, @Value(value="${plc.coarseScreen.rack:0}") int rack, @Value(value="${plc.coarseScreen.slot:1}") int slot) {
        super(connectionPool, "CoarseScreen", "S7-200 SMART SR20", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerCoarseDevice("CS1", "\u7c97\u683c\u68051", 20, 0, 21, 0, 22, 0, 150, 0, 150, 1);
        this.registerCoarseDevice("CS2", "\u7c97\u683c\u68052", 20, 1, 21, 1, 22, 1, 150, 2, 150, 3);
        this.registerCoarseDevice("CS_CONVEYOR", "\u76ae\u5e26\u8f93\u9001\u673a", 20, 2, 21, 2, 22, 2, 150, 4, 150, 5);
        this.registerBoolVParameter("CS_REMOTE_AUTO_START", "\u8fdc\u7a0b\u81ea\u52a8\u542f\u52a8", 50, 0, true);
        this.registerBoolVParameter("CS_LOCAL_AUTO", "\u5c31\u5730\u81ea\u52a8", 150, 6, false);
        this.registerBoolVParameter("CS_REMOTE_CONTROL", "\u8fdc\u7a0b\u63a7\u5236", 150, 7, false);
    }

    @Override
    public boolean writeParameter(String parameterCode, String value) {
        String code;
        String string = code = parameterCode == null ? null : parameterCode.toUpperCase();
        if (this.isPulseParameter(code)) {
            boolean enabled;
            String tagCode = code + "_TAG";
            boolean bl = enabled = Boolean.parseBoolean(value) || "1".equals(value);
            if (!enabled) {
                return this.writeBoolTag(tagCode, false);
            }
            return this.pulseBoolTag(tagCode, 300L);
        }
        return super.writeParameter(parameterCode, value);
    }

    private boolean isPulseParameter(String code) {
        return "CS1_START".equals(code) || "CS2_START".equals(code) || "CS_CONVEYOR_START".equals(code) || "CS1_STOP".equals(code) || "CS2_STOP".equals(code) || "CS_CONVEYOR_STOP".equals(code);
    }

    private void registerCoarseDevice(String code, String name, int manualByte, int manualBit, int startByte, int startBit, int stopByte, int stopBit, int runningByte, int runningBit, int faultByte, int faultBit) {
        String manualTag = this.registerBoolVTag(code + "_MANUAL_TAG", name + "\u624b\u81ea\u52a8\u5207\u6362", manualByte, manualBit, true);
        String startTag = this.registerBoolVTag(code + "_START_TAG", name + "\u624b\u52a8\u542f\u52a8", startByte, startBit, true);
        String stopTag = this.registerBoolVTag(code + "_STOP_TAG", name + "\u624b\u52a8\u505c\u6b62", stopByte, stopBit, true);
        String runningTag = this.registerBoolVTag(code + "_RUNNING_TAG", name + "\u8fd0\u884c", runningByte, runningBit, false);
        String faultTag = this.registerBoolVTag(code + "_FAULT_TAG", name + "\u6545\u969c", faultByte, faultBit, false);
        this.registerDevice(code, name, "coarse-screen", runningTag, startTag, manualTag, faultTag);
        this.registerParameter(code + "_MANUAL_MODE", name + "\u624b\u81ea\u52a8\u5207\u6362", manualTag);
        this.registerParameter(code + "_START", name + "\u624b\u52a8\u542f\u52a8", startTag);
        this.registerParameter(code + "_STOP", name + "\u624b\u52a8\u505c\u6b62", stopTag);
        this.registerParameter(code + "_RUNNING", name + "\u8fd0\u884c", runningTag);
        this.registerParameter(code + "_FAULT", name + "\u6545\u969c", faultTag);
    }
}

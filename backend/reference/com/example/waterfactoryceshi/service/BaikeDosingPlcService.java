/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractS7200SmartPlcService;
import com.example.waterfactoryceshi.service.BaikeDosingPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BaikeDosingPlcService
extends AbstractS7200SmartPlcService {
    public BaikeDosingPlcService(BaikeDosingPlcConnectionPool connectionPool, @Value(value="${plc.baikeDosing.ip}") String plcIp, @Value(value="${plc.baikeDosing.rack:0}") int rack, @Value(value="${plc.baikeDosing.slot:1}") int slot) {
        super(connectionPool, "BaikeDosing", "S7-200 SMART SR30", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerVDevice("BJD_INLET_VALVE", "\u8fdb\u6c34\u7535\u78c1\u9600", "valve", 3000, 6, 400, 0, 500, 0, null, null);
        this.registerVDevice("BJD_FEED_MOTOR", "\u8fdb\u6599\u7535\u673a", "motor", 3000, 7, 400, 1, 500, 1, 3000, 0);
        this.registerVDevice("BJD_MIXER_1", "1#\u6405\u62cc\u673a", "mixer", 3001, 0, 400, 2, 500, 2, 3000, 1);
        this.registerVDevice("BJD_MIXER_2", "2#\u6405\u62cc\u673a", "mixer", 3001, 1, 400, 3, 500, 3, 3000, 2);
        this.registerVDevice("BJD_MIXER_3", "3#\u6405\u62cc\u673a", "mixer", 3001, 2, 400, 4, 500, 4, 3000, 3);
        this.registerVDevice("BJD_HOPPER_VIBRATOR", "\u6599\u4ed3\u632f\u52a8", "vibrator", 3001, 3, 400, 5, 500, 5, null, null);
        this.registerVDevice("BJD_HEATER", "\u52a0\u70ed\u5668", "heater", 3001, 4, 400, 6, 500, 6, null, null);
        this.registerVDevice("BJD_DOSING_PUMP_1", "1#\u52a0\u836f\u6cf5", "pump", 3001, 5, 400, 7, 500, 7, 3000, 4);
        this.registerVDevice("BJD_DOSING_PUMP_2", "2#\u52a0\u836f\u6cf5", "pump", 3001, 6, 401, 0, 501, 0, 3000, 5);
        this.registerRealVParameter("BJD_FEED_MOTOR_FREQ_DISPLAY", "\u8fdb\u6599\u7535\u673a\u9891\u7387\u663e\u793a", 50, false);
        this.registerRealVParameter("BJD_FEED_MOTOR_FREQ_SET", "\u8fdb\u6599\u7535\u673a\u9891\u7387\u7ed9\u5b9a", 1200, true);
        this.registerRealVParameter("BJD_INLET_FLOW_INSTANT", "\u8fdb\u6c34\u77ac\u65f6\u6d41\u91cf\u663e\u793a", 54, false);
        this.registerRealVParameter("BJD_INLET_FLOW_TOTAL", "\u8fdb\u6c34\u7d2f\u8ba1\u6d41\u91cf\u663e\u793a", 58, false);
        this.registerRealVParameter("BJD_LEVEL_DISPLAY", "\u52a0\u836f\u673a\u6db2\u4f4d\u663e\u793a", 62, false);
        this.registerIntVParameter("BJD_VIBRATION_ON_TIME", "\u632f\u52a8\u5f00\u65f6\u95f4\u8bbe\u7f6e", 1000, true);
        this.registerIntVParameter("BJD_VIBRATION_OFF_TIME", "\u632f\u52a8\u5173\u65f6\u95f4\u8bbe\u7f6e", 1002, true);
        this.registerIntVParameter("BJD_HEAT_ON_TIME", "\u52a0\u70ed\u5f00\u65f6\u95f4\u8bbe\u7f6e", 1004, true);
        this.registerIntVParameter("BJD_HEAT_OFF_TIME", "\u52a0\u70ed\u5173\u65f6\u95f4\u8bbe\u7f6e", 1006, true);
        this.registerRealVParameter("BJD_HIGH_LEVEL_SET", "\u9ad8\u6db2\u4f4d\u8bbe\u7f6e", 1204, true);
        this.registerRealVParameter("BJD_MID_LEVEL_SET", "\u4e2d\u6db2\u4f4d\u8bbe\u7f6e", 1208, true);
        this.registerRealVParameter("BJD_LOW_LEVEL_SET", "\u4f4e\u6db2\u4f4d\u8bbe\u7f6e", 1212, true);
        this.registerRealVParameter("BJD_START_FLOW_SET", "\u5f00\u673a\u8fdb\u6c34\u6d41\u91cf\u8bbe\u7f6e", 1216, true);
        this.registerRealVParameter("BJD_STOP_FLOW_SET", "\u505c\u673a\u8fdb\u6c34\u6d41\u91cf\u8bbe\u7f6e", 1220, true);
    }

    private void registerVDevice(String code, String name, String type, int runningByte, int runningBit, int commandByte, int commandBit, int modeByte, int modeBit, Integer faultByte, Integer faultBit) {
        String runningTag = this.registerBoolVTag(code + "_RUNNING_TAG", name + "\u8fd0\u884c", runningByte, runningBit, false);
        String commandTag = this.registerBoolVTag(code + "_COMMAND_TAG", name + "\u8fdc\u7a0b\u542f\u505c\u63a7\u5236", commandByte, commandBit, true);
        String modeTag = this.registerBoolVTag(code + "_MODE_TAG", name + "\u624b\u81ea\u52a8\u8f6c\u6362\u63a7\u5236", modeByte, modeBit, true);
        String faultTag = null;
        if (faultByte != null && faultBit != null) {
            faultTag = this.registerBoolVTag(code + "_FAULT_TAG", name + "\u6545\u969c", faultByte, faultBit, false);
        }
        this.registerDevice(code, name, type, runningTag, commandTag, modeTag, faultTag);
        this.registerParameter(code + "_RUNNING", name + "\u8fd0\u884c", runningTag);
        this.registerParameter(code + "_COMMAND", name + "\u8fdc\u7a0b\u542f\u505c\u63a7\u5236", commandTag);
        this.registerParameter(code + "_MODE", name + "\u624b\u81ea\u52a8\u8f6c\u6362\u63a7\u5236", modeTag);
        if (faultTag != null) {
            this.registerParameter(code + "_FAULT", name + "\u6545\u969c", faultTag);
        }
    }

    private void registerBoolVParameter(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        this.registerParameter(code, name, this.registerBoolVTag(code + "_TAG", name, byteOffset, bitOffset, writable));
    }

    private void registerIntVParameter(String code, String name, int byteOffset, boolean writable) {
        this.registerParameter(code, name, this.registerIntVTag(code + "_TAG", name, byteOffset, writable));
    }

    private void registerRealVParameter(String code, String name, int byteOffset, boolean writable) {
        this.registerParameter(code, name, this.registerRealVTag(code + "_TAG", name, byteOffset, writable));
    }
}

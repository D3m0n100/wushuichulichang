/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.BaikeFiberDiscPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BaikeFiberDiscPlcService
extends AbstractWastewaterSmartPlcService {
    public BaikeFiberDiscPlcService(BaikeFiberDiscPlcConnectionPool connectionPool, @Value(value="${plc.baikeFiberDisc.ip}") String plcIp, @Value(value="${plc.baikeFiberDisc.rack:0}") int rack, @Value(value="${plc.baikeFiberDisc.slot:1}") int slot) {
        super(connectionPool, "BaikeFiberDisc", "S7-200 SMART SR60", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerVDevice("BFD_1_DRIVE_MOTOR", "1#\u8f6c\u76d8\u4f20\u52a8\u7535\u673a", "motor", 3000, 6, 400, 0, true, 3000, 0);
        this.registerVDevice("BFD_1_BACKWASH_PUMP_1", "1#\u8f6c\u76d8\u53cd\u51b2\u6cf51", "pump", 3000, 7, 400, 1, true, 3000, 1);
        this.registerVDevice("BFD_1_BACKWASH_PUMP_2", "1#\u8f6c\u76d8\u53cd\u51b2\u6cf52", "pump", 3001, 0, 400, 2, true, 3000, 2);
        this.registerVDevice("BFD_1_BACKWASH_VALVE_1", "1#\u8f6c\u76d8\u53cd\u51b2\u96001", "valve", 3001, 1, 400, 3, false, 700, 3);
        this.registerVDevice("BFD_1_BACKWASH_VALVE_2", "1#\u8f6c\u76d8\u53cd\u51b2\u96002", "valve", 3001, 2, 400, 4, false, 700, 4);
        this.registerVDevice("BFD_1_BACKWASH_VALVE_3", "1#\u8f6c\u76d8\u53cd\u51b2\u96003", "valve", 3001, 3, 400, 5, false, 700, 5);
        this.registerVDevice("BFD_1_BACKWASH_VALVE_4", "1#\u8f6c\u76d8\u53cd\u51b2\u96004", "valve", 3001, 4, 400, 6, false, 700, 6);
        this.registerVDevice("BFD_1_SLUDGE_VALVE_1", "1#\u8f6c\u76d8\u6392\u6ce5\u96001", "valve", 3001, 5, 401, 1, false, 701, 1);
        this.registerVDevice("BFD_1_SLUDGE_VALVE_2", "1#\u8f6c\u76d8\u6392\u6ce5\u96002", "valve", 3001, 6, 401, 2, false, 701, 2);
        this.registerBoolVParameter("BFD_1_MODE_SWITCH", "1#\u8f6c\u76d8\u624b\u81ea\u52a8\u8f6c\u6362", 500, 0, true);
        this.registerVDevice("BFD_2_DRIVE_MOTOR", "2#\u8f6c\u76d8\u4f20\u52a8\u7535\u673a", "motor", 3001, 7, 401, 3, true, 3000, 3);
        this.registerVDevice("BFD_2_BACKWASH_PUMP_1", "2#\u8f6c\u76d8\u53cd\u51b2\u6cf51", "pump", 3002, 0, 401, 4, true, 3000, 4);
        this.registerVDevice("BFD_2_BACKWASH_PUMP_2", "2#\u8f6c\u76d8\u53cd\u51b2\u6cf52", "pump", 3002, 1, 401, 5, true, 3000, 5);
        this.registerVDevice("BFD_2_BACKWASH_VALVE_1", "2#\u8f6c\u76d8\u53cd\u51b2\u96001", "valve", 3002, 2, 401, 6, false, 701, 6);
        this.registerVDevice("BFD_2_BACKWASH_VALVE_2", "2#\u8f6c\u76d8\u53cd\u51b2\u96002", "valve", 3002, 3, 401, 7, false, 701, 7);
        this.registerVDevice("BFD_2_BACKWASH_VALVE_3", "2#\u8f6c\u76d8\u53cd\u51b2\u96003", "valve", 3002, 4, 402, 0, false, 702, 0);
        this.registerVDevice("BFD_2_BACKWASH_VALVE_4", "2#\u8f6c\u76d8\u53cd\u51b2\u96004", "valve", 3002, 5, 402, 1, false, 702, 1);
        this.registerVDevice("BFD_2_BACKWASH_VALVE_5", "2#\u8f6c\u76d8\u53cd\u6392\u6ce51", "valve", 3002, 6, 402, 4, false, 702, 4);
        this.registerVDevice("BFD_2_BACKWASH_VALVE_6", "2#\u8f6c\u76d8\u53cd\u6392\u6ce52", "valve", 3002, 7, 402, 5, false, 702, 5);
        this.registerBoolVParameter("BFD_2_MODE_SWITCH", "2#\u8f6c\u76d8\u624b\u81ea\u52a8\u8f6c\u6362", 500, 1, true);
        this.registerRealVParameter("BFD_1_LEVEL_DISPLAY", "1#\u8f6c\u76d8\u6db2\u4f4d\u663e\u793a", 50, false);
        this.registerRealVParameter("BFD_2_LEVEL_DISPLAY", "2#\u8f6c\u76d8\u6db2\u4f4d\u663e\u793a", 54, false);
        this.registerRealVParameter("BFD_LEVEL_RANGE_SET", "\u6db2\u4f4d\u6d4b\u91cf\u8303\u56f4\u8bbe\u7f6e", 1100, true);
        this.registerRealVParameter("BFD_1_HIGH_LEVEL_SET", "1#\u8f6c\u76d8\u9ad8\u6db2\u4f4d\u8bbe\u7f6e", 1104, true);
        this.registerRealVParameter("BFD_1_LOW_LEVEL_SET", "1#\u8f6c\u76d8\u4f4e\u6db2\u4f4d\u8bbe\u7f6e", 1108, true);
        this.registerRealVParameter("BFD_2_HIGH_LEVEL_SET", "2#\u8f6c\u76d8\u9ad8\u6db2\u4f4d\u8bbe\u7f6e", 1112, true);
        this.registerRealVParameter("BFD_2_LOW_LEVEL_SET", "2#\u8f6c\u76d8\u4f4e\u6db2\u4f4d\u8bbe\u7f6e", 1116, true);
        this.registerRealVParameter("BFD_1_SUPER_HIGH_LEVEL_SET", "1#\u8f6c\u76d8\u8d85\u9ad8\u6db2\u4f4d\u8bbe\u7f6e", 1120, true);
        this.registerRealVParameter("BFD_2_SUPER_HIGH_LEVEL_SET", "2#\u8f6c\u76d8\u8d85\u9ad8\u6db2\u4f4d\u8bbe\u7f6e", 1124, true);
        this.registerIntVParameter("BFD_FILTER_TIME_SET", "\u8fc7\u6ee4\u65f6\u95f4\u8bbe\u7f6e", 1000, true);
        this.registerIntVParameter("BFD_BACKWASH_TIME_SET", "\u53cd\u51b2\u65f6\u95f4\u8bbe\u7f6e", 1002, true);
        this.registerRealVParameter("BFD_PUMP_NEG_PRESSURE_1", "1#\u51b2\u6d17\u6cf5\u8d1f\u538b\u529b\u663e\u793a", 650, false);
        this.registerRealVParameter("BFD_PUMP_NEG_PRESSURE_2", "2#\u51b2\u6d17\u6cf5\u8d1f\u538b\u529b\u663e\u793a", 654, false);
        this.registerRealVParameter("BFD_PUMP_NEG_PRESSURE_3", "3#\u51b2\u6d17\u6cf5\u8d1f\u538b\u529b\u663e\u793a", 658, false);
        this.registerRealVParameter("BFD_PUMP_NEG_PRESSURE_4", "4#\u51b2\u6d17\u6cf5\u8d1f\u538b\u529b\u663e\u793a", 662, false);
    }

    private void registerVDevice(String code, String name, String type, int runningByte, int runningBit, int commandByte, int commandBit, boolean commandWritable, Integer faultByte, Integer faultBit) {
        String runningTag = this.registerBoolVTag(code + "_RUNNING_TAG", name + "\u8fd0\u884c", runningByte, runningBit, false);
        String commandTag = this.registerBoolVTag(code + "_COMMAND_TAG", name + "\u5f00\u5173\u5730\u5740", commandByte, commandBit, commandWritable);
        String faultTag = null;
        if (faultByte != null && faultBit != null) {
            faultTag = this.registerBoolVTag(code + "_FAULT_TAG", name + "\u6545\u969c", faultByte, faultBit, false);
        }
        this.registerDevice(code, name, type, runningTag, commandTag, null, faultTag);
        this.registerParameter(code + "_RUNNING", name + "\u8fd0\u884c", runningTag);
        this.registerParameter(code + "_COMMAND", name + "\u5f00\u5173\u5730\u5740", commandTag);
        if (faultTag != null) {
            this.registerParameter(code + "_FAULT", name + "\u6545\u969c", faultTag);
        }
    }
}

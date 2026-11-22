/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.SodiumAcetatePlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SodiumAcetatePlcService
extends AbstractWastewaterSmartPlcService {
    public SodiumAcetatePlcService(SodiumAcetatePlcConnectionPool connectionPool, @Value(value="${plc.sodiumAcetate.ip}") String plcIp, @Value(value="${plc.sodiumAcetate.rack:0}") int rack, @Value(value="${plc.sodiumAcetate.slot:1}") int slot) {
        super(connectionPool, "SodiumAcetate", "S7-200 SMART SR40+EM AE04", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerInputStatusDevice("SAC_MIXER_1", "\u6405\u62cc\u673a1", "mixer", 0, 0, 0, 1);
        this.registerInputStatusDevice("SAC_MIXER_2", "\u6405\u62cc\u673a2", "mixer", 0, 2, 0, 3);
        this.registerInputStatusDevice("SAC_UNLOAD_1", "\u5378\u6599\u6cf51", "pump", 0, 4, 0, 5);
        this.registerInputStatusDevice("SAC_UNLOAD_2", "\u5378\u6599\u6cf52", "pump", 0, 6, 0, 7);
        this.registerInputStatusDevice("SAC_DOSING_1", "\u8ba1\u91cf\u6cf51", "pump", 1, 0, 1, 1);
        this.registerInputStatusDevice("SAC_DOSING_2", "\u8ba1\u91cf\u6cf52", "pump", 1, 2, 1, 3);
        this.registerInputStatusDevice("SAC_DOSING_3", "\u8ba1\u91cf\u6cf53", "pump", 1, 4, 1, 5);
        this.registerInputStatusDevice("SAC_VALVE_1", "\u51fa\u6db2\u7535\u78c1\u96001", "valve", 1, 6, null, null);
        this.registerInputStatusDevice("SAC_VALVE_2", "\u51fa\u6db2\u7535\u78c1\u96002", "valve", 1, 7, null, null);
        this.registerInputStatusDevice("SAC_VALVE_3", "\u51fa\u6db2\u7535\u78c1\u96003", "valve", 2, 0, null, null);
        this.registerInputStatusDevice("SAC_VALVE_4", "\u51fa\u6db2\u7535\u78c1\u96004", "valve", 2, 1, null, null);
        this.registerInputStatusDevice("SAC_DOSING_4", "\u8ba1\u91cf\u6cf54", "pump", 2, 4, 2, 5);
        this.registerBoolInputParameter("SAC_LOCAL_AUTO", "\u5c31\u5730\u81ea\u52a8", 2, 2);
        this.registerBoolInputParameter("SAC_REMOTE_CONTROL", "\u8fdc\u7a0b\u63a7\u5236", 2, 3);
        this.registerRealVParameter("SAC_LEVEL1_UPPER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e0a\u9650\u503c", 1100, true);
        this.registerRealVParameter("SAC_LEVEL1_LOWER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e0b\u9650\u503c", 1104, true);
        this.registerRealVParameter("SAC_LEVEL1_OUTPUT", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u8f93\u51fa\u503c", 1108, false);
        this.registerRealVParameter("SAC_LEVEL1_HIGH", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u9ad8\u8bbe\u5b9a\u503c", 1116, true);
        this.registerRealVParameter("SAC_LEVEL1_MID", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e2d\u8bbe\u5b9a\u503c", 1120, true);
        this.registerRealVParameter("SAC_LEVEL1_LOW", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4f4e\u8bbe\u5b9a\u503c", 1124, true);
        this.registerRealVParameter("SAC_LEVEL2_UPPER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d2\u4eea\u8868\u4e0a\u9650\u503c", 1134, true);
        this.registerRealVParameter("SAC_LEVEL2_LOWER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d2\u4eea\u8868\u4e0b\u9650\u503c", 1138, true);
        this.registerRealVParameter("SAC_LEVEL2_OUTPUT", "\u8d85\u58f0\u6ce2\u6db2\u4f4d2\u8f93\u51fa\u503c", 1142, false);
        this.registerRealVParameter("SAC_LEVEL2_HIGH", "\u8d85\u58f0\u6ce2\u6db2\u4f4d2\u4eea\u8868\u9ad8\u8bbe\u5b9a\u503c", 1146, true);
        this.registerRealVParameter("SAC_LEVEL2_MID", "\u8d85\u58f0\u6ce2\u6db2\u4f4d2\u4eea\u8868\u4e2d\u8bbe\u5b9a\u503c", 1150, true);
        this.registerRealVParameter("SAC_LEVEL2_LOW", "\u8d85\u58f0\u6ce2\u6db2\u4f4d2\u4eea\u8868\u4f4e\u8bbe\u5b9a\u503c", 1154, true);
        this.registerBoolVParameter("SAC_REMOTE_AUTO_SIGNAL", "\u8fdc\u7a0b\u81ea\u52a8\u4fe1\u53f7", 50, 4, true);
        this.registerBoolVParameter("SAC_DOSING1_REMOTE_START", "\u8ba1\u91cf\u6cf51\u8fdc\u7a0b\u542f\u52a8\u4fe1\u53f7", 62, 1, true);
        this.registerBoolVParameter("SAC_DOSING2_REMOTE_START", "\u8ba1\u91cf\u6cf52\u8fdc\u7a0b\u542f\u52a8\u4fe1\u53f7", 62, 2, true);
        this.registerBoolVParameter("SAC_DOSING3_REMOTE_START", "\u8ba1\u91cf\u6cf53\u8fdc\u7a0b\u542f\u52a8\u4fe1\u53f7", 62, 3, true);
        this.registerBoolVParameter("SAC_DOSING4_REMOTE_START", "\u8ba1\u91cf\u6cf54\u8fdc\u7a0b\u542f\u52a8\u4fe1\u53f7", 62, 4, true);
    }
}

/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.SodiumHypochloritePlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SodiumHypochloritePlcService
extends AbstractWastewaterSmartPlcService {
    public SodiumHypochloritePlcService(SodiumHypochloritePlcConnectionPool connectionPool, @Value(value="${plc.sodiumHypochlorite.ip}") String plcIp, @Value(value="${plc.sodiumHypochlorite.rack:0}") int rack, @Value(value="${plc.sodiumHypochlorite.slot:1}") int slot) {
        super(connectionPool, "SodiumHypochlorite", "S7-200 SMART SR40+EM AE08+EM AO04", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerInputStatusDevice("SHC_MIXER_1", "\u6405\u62cc\u673a1", "mixer", 0, 0, 0, 1);
        this.registerInputStatusDevice("SHC_MIXER_2", "\u6405\u62cc\u673a2", "mixer", 0, 2, 0, 3);
        this.registerInputStatusDevice("SHC_UNLOAD_1", "\u5378\u6599\u6cf51", "pump", 0, 4, 0, 5);
        this.registerInputStatusDevice("SHC_UNLOAD_2", "\u5378\u6599\u6cf52", "pump", 0, 6, 0, 7);
        this.registerInputStatusDevice("SHC_SCREW_1", "\u87ba\u6746\u8ba1\u91cf\u6cf51", "pump", 1, 0, null, null);
        this.registerInputStatusDevice("SHC_SCREW_2", "\u87ba\u6746\u8ba1\u91cf\u6cf52", "pump", 1, 1, null, null);
        this.registerInputStatusDevice("SHC_SCREW_3", "\u87ba\u6746\u8ba1\u91cf\u6cf53", "pump", 1, 2, null, null);
        this.registerBoolInputParameter("SHC_LOCAL_AUTO", "\u5c31\u5730\u81ea\u52a8", 1, 7);
        this.registerBoolInputParameter("SHC_REMOTE_CONTROL", "\u8fdc\u7a0b\u63a7\u5236", 2, 0);
        this.registerRealVParameter("SHC_LEVEL1_UPPER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e0a\u9650\u503c", 1104, true);
        this.registerRealVParameter("SHC_LEVEL1_LOWER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e0b\u9650\u503c", 1108, false);
        this.registerRealVParameter("SHC_LEVEL1_OUTPUT", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u8f93\u51fa\u503c", 1116, true);
        this.registerRealVParameter("SHC_LEVEL1_MID", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e2d\u8bbe\u5b9a\u503c", 1120, true);
        this.registerRealVParameter("SHC_LEVEL1_LOW", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4f4e\u8bbe\u5b9a\u503c", 1124, true);
        this.registerIntVParameter("SHC_SWITCH_TIME", "\u8ba1\u91cf\u6cf5\u5207\u6362\u65f6\u95f4", 2006, true);
        this.registerBoolVParameter("SHC_REMOTE_AUTO_SIGNAL", "\u8fdc\u7a0b\u81ea\u52a8\u4fe1\u53f7", 50, 0, true);
        this.registerBoolVParameter("SHC_REMOTE_START_1", "\u8fdc\u7a0b\u542f\u6cf5\u4fe1\u53f71", 50, 2, true);
        this.registerBoolVParameter("SHC_REMOTE_START_2", "\u8fdc\u7a0b\u542f\u6cf5\u4fe1\u53f72", 50, 3, true);
        this.registerBoolVParameter("SHC_REMOTE_FAULT_1", "\u8fdc\u7a0b\u6545\u969c\u4fe1\u53f71", 60, 0, true);
        this.registerBoolVParameter("SHC_REMOTE_FAULT_2", "\u8fdc\u7a0b\u6545\u969c\u4fe1\u53f72", 60, 1, true);
        this.registerRealVParameter("SHC_DOSING_FREQ_1", "\u52a0\u836f\u6cf51\u9891\u7387\u7ed9\u5b9a", 3000, true);
        this.registerRealVParameter("SHC_DOSING_FREQ_2", "\u52a0\u836f\u6cf52\u9891\u7387\u7ed9\u5b9a", 3004, true);
        this.registerRealVParameter("SHC_DOSING_FREQ_3", "\u52a0\u836f\u6cf53\u9891\u7387\u7ed9\u5b9a", 3008, true);
    }
}

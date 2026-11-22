/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.PacPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PacPlcService
extends AbstractWastewaterSmartPlcService {
    public PacPlcService(PacPlcConnectionPool connectionPool, @Value(value="${plc.pac.ip}") String plcIp, @Value(value="${plc.pac.rack:0}") int rack, @Value(value="${plc.pac.slot:1}") int slot) {
        super(connectionPool, "PAC", "S7-200 SMART SR30+EM AE08", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerInputStatusDevice("PAC_MIXER_1", "\u6405\u62cc\u673a1", "mixer", 0, 0, 0, 1);
        this.registerInputStatusDevice("PAC_MIXER_2", "\u6405\u62cc\u673a2", "mixer", 0, 2, 0, 3);
        this.registerInputStatusDevice("PAC_UNLOAD_1", "PAC\u5378\u6599\u6cf51", "pump", 0, 4, 0, 5);
        this.registerInputStatusDevice("PAC_UNLOAD_2", "PAC\u5378\u6599\u6cf52", "pump", 0, 6, 0, 7);
        this.registerInputStatusDevice("PAC_DOSING_1", "\u8ba1\u91cf\u6cf51", "pump", 1, 0, 1, 1);
        this.registerInputStatusDevice("PAC_DOSING_2", "\u8ba1\u91cf\u6cf52", "pump", 1, 2, 1, 3);
        this.registerInputStatusDevice("PAC_VALVE_1", "\u51fa\u836f\u96001", "valve", 1, 4, null, null);
        this.registerInputStatusDevice("PAC_VALVE_2", "\u51fa\u836f\u96002", "valve", 1, 5, null, null);
        this.registerBoolInputParameter("PAC_LOCAL_AUTO", "\u5c31\u5730\u81ea\u52a8", 1, 6);
        this.registerBoolInputParameter("PAC_REMOTE_CONTROL", "\u8fdc\u7a0b\u63a7\u5236", 1, 7);
        this.registerRealVParameter("PAC_LEVEL1_UPPER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e0a\u9650\u503c", 1100, true);
        this.registerRealVParameter("PAC_LEVEL1_LOWER", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e0b\u9650\u503c", 1104, true);
        this.registerRealVParameter("PAC_LEVEL1_OUTPUT", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u8f93\u51fa\u503c", 1108, false);
        this.registerRealVParameter("PAC_LEVEL1_HIGH", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u9ad8\u8bbe\u5b9a\u503c", 1116, true);
        this.registerRealVParameter("PAC_LEVEL1_MID", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4e2d\u8bbe\u5b9a\u503c", 1120, true);
        this.registerRealVParameter("PAC_LEVEL1_LOW", "\u8d85\u58f0\u6ce2\u6db2\u4f4d1\u4eea\u8868\u4f4e\u8bbe\u5b9a\u503c", 1124, true);
        this.registerIntVParameter("PAC_SWITCH_TIME", "\u8ba1\u91cf\u6cf5\u5207\u6362\u65f6\u95f4", 2006, true);
        this.registerBoolVParameter("PAC_REMOTE_AUTO_START", "\u8fdc\u7a0b\u81ea\u52a8\u542f\u52a8", 50, 0, true);
        this.registerBoolVParameter("PAC_VALVE12_REMOTE_START", "\u7535\u78c1\u960012\u8fdc\u7a0b\u542f\u52a8", 50, 1, true);
    }
}

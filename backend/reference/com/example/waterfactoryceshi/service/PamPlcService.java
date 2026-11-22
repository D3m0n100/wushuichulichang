/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.PamPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PamPlcService
extends AbstractWastewaterSmartPlcService {
    public PamPlcService(PamPlcConnectionPool connectionPool, @Value(value="${plc.pam.ip}") String plcIp, @Value(value="${plc.pam.rack:0}") int rack, @Value(value="${plc.pam.slot:1}") int slot) {
        super(connectionPool, "PAM", "S7-200 SMART SR40+EM AE04", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerInputStatusDevice("PAM_MIXER_1", "\u6405\u62cc\u673a1", "mixer", 0, 0, 0, 1);
        this.registerInputStatusDevice("PAM_MIXER_2", "\u6405\u62cc\u673a2", "mixer", 0, 2, 0, 3);
        this.registerInputStatusDevice("PAM_MIXER_3", "\u6405\u62cc\u673a3", "mixer", 0, 4, 0, 5);
        this.registerInputStatusDevice("PAM_FEEDER", "\u6295\u6599\u673a", "feeder", 0, 6, 0, 7);
        this.registerInputStatusDevice("PAM_SCREW_1", "\u87ba\u6746\u8ba1\u91cf\u6cf51", "pump", 1, 0, 1, 1);
        this.registerInputStatusDevice("PAM_SCREW_2", "\u87ba\u6746\u8ba1\u91cf\u6cf52", "pump", 1, 2, 1, 3);
        this.registerInputStatusDevice("PAM_SCREW_3", "\u87ba\u6746\u8ba1\u91cf\u6cf53", "pump", 1, 4, 1, 5);
        this.registerInputStatusDevice("PAM_VIBRATOR", "\u632f\u52a8\u673a", "vibrator", 1, 7, null, null);
        this.registerInputStatusDevice("PAM_INLET_VALVE", "\u8fdb\u6c34\u7535\u78c1\u9600", "valve", 2, 0, null, null);
        this.registerInputStatusDevice("PAM_OUTLET_VALVE_1", "\u51fa\u6db2\u7535\u78c1\u96001", "valve", 2, 1, null, null);
        this.registerInputStatusDevice("PAM_OUTLET_VALVE_2", "\u51fa\u6db2\u7535\u78c1\u96002", "valve", 2, 2, null, null);
        this.registerBoolInputParameter("PAM_REMOTE_SIGNAL", "\u8fdc\u7a0b\u4fe1\u53f7", 1, 6);
        this.registerBoolInputParameter("PAM_LEVEL", "\u6599\u4f4d", 2, 3);
        this.registerBoolInputParameter("PAM_TANK_LOW", "\u836f\u7bb1\u4f4e", 2, 4);
        this.registerBoolInputParameter("PAM_TANK_MID", "\u836f\u7bb1\u4e2d", 2, 5);
        this.registerBoolInputParameter("PAM_TANK_HIGH", "\u836f\u7bb1\u9ad8", 2, 6);
        this.registerBoolInputParameter("PAM_LOCAL_AUTO", "\u5c31\u5730\u81ea\u52a8", 2, 7);
        this.registerIntVParameter("PAM_VIBRATOR_RUN_TIME", "\u632f\u52a8\u5668\u8fd0\u884c\u65f6\u95f4", 2000, true);
        this.registerIntVParameter("PAM_VIBRATOR_STOP_TIME", "\u632f\u52a8\u5668\u505c\u6b62\u65f6\u95f4", 2002, true);
        this.registerBoolVParameter("PAM_REMOTE_AUTO_SIGNAL", "\u8fdc\u7a0b\u81ea\u52a8\u4fe1\u53f7", 50, 2, true);
        this.registerBoolVParameter("PAM_REMOTE_START_1", "\u8fdc\u7a0b\u542f\u6cf5\u4fe1\u53f71", 50, 0, true);
        this.registerBoolVParameter("PAM_REMOTE_START_2", "\u8fdc\u7a0b\u542f\u6cf5\u4fe1\u53f72", 50, 1, true);
    }
}

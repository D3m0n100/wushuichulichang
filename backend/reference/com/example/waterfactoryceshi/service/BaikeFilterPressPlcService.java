/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.BaikeFilterPressPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BaikeFilterPressPlcService
extends AbstractWastewaterSmartPlcService {
    private static final long DEFAULT_PULSE_MS = 300L;

    public BaikeFilterPressPlcService(BaikeFilterPressPlcConnectionPool connectionPool, @Value(value="${plc.baikeFilterPress.ip}") String plcIp, @Value(value="${plc.baikeFilterPress.rack:0}") int rack, @Value(value="${plc.baikeFilterPress.slot:1}") int slot) {
        super(connectionPool, "BaikeFilterPress", "S7-200 SMART SR40", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerVStatusDevice("BFP_1_MAIN_MOTOR", "1#\u5e26\u673a\u4e3b\u7535\u673a", "motor", 3001, 4, 3000, 0);
        this.registerVStatusDevice("BFP_1_THICKENER_MOTOR", "1#\u5e26\u673a\u6d53\u7f29\u7535\u673a", "motor", 3001, 5, 3000, 1);
        this.registerVStatusDevice("BFP_1_BACKWASH_PUMP", "1#\u5e26\u673a\u53cd\u51b2\u6cf5", "pump", 3001, 6, 3000, 2);
        this.registerVStatusDevice("BFP_1_AIR_COMPRESSOR", "1#\u5e26\u673a\u7a7a\u538b\u673a", "compressor", 3001, 7, 3000, 3);
        this.registerVStatusDevice("BFP_1_VALVE", "1#\u5e26\u673a\u7535\u52a8\u9600", "valve", 3002, 0, null, null);
        this.registerVStatusDevice("BFP_1_SLUDGE_PUMP", "1#\u5e26\u673a\u8fdb\u6ce5\u6cf5", "pump", 3002, 1, 3000, 4);
        this.registerVStatusDevice("BFP_1_FLOCC_MIXER", "1#\u5e26\u673a\u7d6e\u51dd\u6405\u62cc", "mixer", 3002, 2, 3000, 5);
        this.registerVStatusDevice("BFP_2_MAIN_MOTOR", "2#\u5e26\u673a\u4e3b\u7535\u673a", "motor", 3002, 3, 3000, 6);
        this.registerVStatusDevice("BFP_2_THICKENER_MOTOR", "2#\u5e26\u673a\u6d53\u7f29\u7535\u673a", "motor", 3002, 4, 3000, 7);
        this.registerVStatusDevice("BFP_2_BACKWASH_PUMP", "2#\u5e26\u673a\u53cd\u51b2\u6cf5", "pump", 3002, 5, 3001, 0);
        this.registerVStatusDevice("BFP_2_AIR_COMPRESSOR", "2#\u5e26\u673a\u7a7a\u538b\u673a", "compressor", 3002, 6, 3001, 1);
        this.registerVStatusDevice("BFP_2_VALVE", "2#\u5e26\u673a\u7535\u52a8\u9600", "valve", 3002, 7, null, null);
        this.registerVStatusDevice("BFP_2_SLUDGE_PUMP", "2#\u5e26\u673a\u8fdb\u6ce5\u6cf5", "pump", 3003, 0, 3001, 2);
        this.registerVStatusDevice("BFP_2_FLOCC_MIXER", "2#\u5e26\u673a\u7d6e\u51dd\u6405\u62cc", "mixer", 3003, 1, 3001, 3);
        this.registerBoolVParameter("BFP_1_ONE_KEY_START", "1#\u5e26\u673a\u4e00\u952e\u542f\u52a8", 0, 0, true);
        this.registerBoolVParameter("BFP_1_ONE_KEY_STOP", "1#\u5e26\u673a\u4e00\u952e\u505c\u6b62", 0, 1, true);
        this.registerBoolVParameter("BFP_2_ONE_KEY_START", "2#\u5e26\u673a\u4e00\u952e\u542f\u52a8", 0, 2, true);
        this.registerBoolVParameter("BFP_2_ONE_KEY_STOP", "2#\u5e26\u673a\u4e00\u952e\u505c\u6b62", 0, 3, true);
        this.registerRealVParameter("BFP_1_MAIN_FREQ_DISPLAY", "1#\u5e26\u673a\u4e3b\u673a\u9891\u7387\u663e\u793a", 50, false);
        this.registerRealVParameter("BFP_1_THICKENER_FREQ_DISPLAY", "1#\u5e26\u673a\u6d53\u7f29\u673a\u9891\u7387\u663e\u793a", 54, false);
        this.registerRealVParameter("BFP_2_MAIN_FREQ_DISPLAY", "2#\u5e26\u673a\u4e3b\u673a\u9891\u7387\u663e\u793a", 58, false);
        this.registerRealVParameter("BFP_2_THICKENER_FREQ_DISPLAY", "2#\u5e26\u673a\u6d53\u7f29\u673a\u9891\u7387\u663e\u793a", 62, false);
        this.registerRealVParameter("BFP_1_MAIN_FREQ_SET", "1#\u5e26\u673a\u4e3b\u673a\u9891\u7387\u7ed9\u5b9a", 1100, true);
        this.registerRealVParameter("BFP_1_THICKENER_FREQ_SET", "1#\u5e26\u673a\u6d53\u7f29\u673a\u9891\u7387\u7ed9\u5b9a", 1104, true);
        this.registerRealVParameter("BFP_2_MAIN_FREQ_SET", "2#\u5e26\u673a\u4e3b\u673a\u9891\u7387\u7ed9\u5b9a", 1108, true);
        this.registerRealVParameter("BFP_2_THICKENER_FREQ_SET", "2#\u5e26\u673a\u6d53\u7f29\u673a\u9891\u7387\u7ed9\u5b9a", 1112, true);
        this.registerIntVParameter("BFP_AIR_DELAY_AFTER_START", "\u7a7a\u538b\u673a\u542f\u52a8\u540e\u5176\u4f59\u8bbe\u5907\u5ef6\u65f6\u5f00\u542f\u65f6\u95f4\u7ed9\u5b9a", 1000, true);
    }

    @Override
    public boolean writeParameter(String parameterCode, String value) {
        String code;
        String string = code = parameterCode == null ? null : parameterCode.toUpperCase();
        if (this.isPulseParameter(code)) {
            boolean enabled;
            String tagCode = this.resolveParameterTag(code);
            if (tagCode == null) {
                throw new IllegalArgumentException("Unknown parameter: " + code);
            }
            boolean bl = enabled = Boolean.parseBoolean(value) || "1".equals(value);
            if (!enabled) {
                return this.writeBoolTag(tagCode, false);
            }
            return this.pulseBoolTag(tagCode, 300L);
        }
        return super.writeParameter(parameterCode, value);
    }

    private void registerVStatusDevice(String code, String name, String type, int runningByte, int runningBit, Integer faultByte, Integer faultBit) {
        String runningTag = this.registerBoolVTag(code + "_RUNNING_TAG", name + "\u8fd0\u884c", runningByte, runningBit, false);
        String faultTag = null;
        if (faultByte != null && faultBit != null) {
            faultTag = this.registerBoolVTag(code + "_FAULT_TAG", name + "\u6545\u969c", faultByte, faultBit, false);
        }
        this.registerDevice(code, name, type, runningTag, null, null, faultTag);
        this.registerParameter(code + "_RUNNING", name + "\u8fd0\u884c", runningTag);
        if (faultTag != null) {
            this.registerParameter(code + "_FAULT", name + "\u6545\u969c", faultTag);
        }
    }

    private boolean isPulseParameter(String code) {
        return "BFP_1_ONE_KEY_START".equals(code) || "BFP_1_ONE_KEY_STOP".equals(code) || "BFP_2_ONE_KEY_START".equals(code) || "BFP_2_ONE_KEY_STOP".equals(code);
    }

    private String resolveParameterTag(String code) {
        String tagCode = code + "_TAG";
        return this.tags.containsKey(tagCode) ? tagCode : null;
    }
}

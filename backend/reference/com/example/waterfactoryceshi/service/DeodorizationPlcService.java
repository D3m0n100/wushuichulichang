/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractS7200SmartPlcService;
import com.example.waterfactoryceshi.service.DeodorizationPlcConnectionPool;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeodorizationPlcService
extends AbstractS7200SmartPlcService {
    private static final long DEFAULT_PULSE_MS = 300L;
    private static final Set<String> PULSE_PARAMETERS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("DEO_FAN_START_V3_4", "DEO_FAN_STOP_V3_5", "DEO_PREWASH_PUMP_1_STOP_V0_3", "DEO_PREWASH_PUMP_2_START_V0_4", "DEO_PREWASH_PUMP_2_STOP_V0_5", "DEO_HUMIDIFIER_RESET_V1000_6", "DEO_BIO_HEATER_MANUAL_STOP_V4_6", "DEO_BIO_HEATER_MANUAL_START_V4_5", "DEO_PREWASH_HEATER_MANUAL_STOP_V4_2", "DEO_PREWASH_HEATER_MANUAL_START_V4_1", "DEO_PREWASH_FILL_OPEN_V6_1", "DEO_PREWASH_FILL_CLOSE_V6_4", "DEO_PREWASH_FILL_STOP_V6_2", "DEO_PREWASH_DRAIN_OPEN_V7_1", "DEO_PREWASH_DRAIN_CLOSE_V7_4", "DEO_PREWASH_DRAIN_STOP_V7_2", "DEO_BIO_FILL_OPEN_V8_1", "DEO_BIO_FILL_CLOSE_V8_4", "DEO_BIO_FILL_STOP_V8_2", "DEO_BIO_DRAIN_OPEN_V10_1", "DEO_BIO_DRAIN_CLOSE_V10_4", "DEO_BIO_DRAIN_STOP_V10_2")));
    private static final Set<String> TOGGLE_PARAMETERS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("DEO_HUMIDIFIER_MANUAL_V9_6", "DEO_HUMIDIFIER_MODE_SWITCH_V20_0", "DEO_BIO_HEATER_MODE_SWITCH_V4_4", "DEO_PREWASH_HEATER_MODE_SWITCH_V4_0", "DEO_PREWASH_FILL_MODE_SWITCH_V6_0", "DEO_PREWASH_DRAIN_MODE_SWITCH_V7_0", "DEO_BIO_FILL_MODE_SWITCH_V8_0", "DEO_BIO_DRAIN_MODE_SWITCH_V10_0", "DEO_TOUCHSCREEN_ESTOP_M0_1")));

    public DeodorizationPlcService(DeodorizationPlcConnectionPool deodorizationPlcConnectionPool, @Value(value="${plc.deodorization.ip}") String string, @Value(value="${plc.deodorization.rack:0}") int n, @Value(value="${plc.deodorization.slot:1}") int n2) {
        super(deodorizationPlcConnectionPool, "Deodorization", "S7-200 SMART SR40", string, n, n2);
    }

    @Override
    protected void initDefinitions() {
        this.registerReadOnlyMotorDevice("DEO_FAN", "\u98ce\u673a", 0, 1, 0, 2);
        this.registerReadOnlyMotorDevice("DEO_PREWASH_PUMP_1", "\u9884\u6d17\u4e00\u6cf5", 0, 3, 0, 4);
        this.registerReadOnlyMotorDevice("DEO_PREWASH_PUMP_2", "\u9884\u6d17\u4e8c\u6cf5", 0, 5, 0, 6);
        this.registerReadOnlyMotorDevice("DEO_BIO_PUMP", "\u751f\u7269\u6cf5", 0, 7, 1, 0);
        this.registerReadOnlyMotorDevice("DEO_PREWASH_HEATER", "\u9884\u6d17\u7535\u52a0\u70ed", 1, 1, null, null);
        this.registerReadOnlyMotorDevice("DEO_BIO_HEATER", "\u751f\u7269\u7535\u52a0\u70ed", 1, 2, null, null);
        this.registerReadOnlyValveDevice("DEO_PREWASH_FILL_VALVE", "\u9884\u6d17\u8865\u6c34\u9600", 1, 3, 1, 4);
        this.registerReadOnlyValveDevice("DEO_PREWASH_DRAIN_VALVE", "\u9884\u6d17\u6392\u6c34\u9600", 1, 5, 1, 6);
        this.registerReadOnlyValveDevice("DEO_BIO_FILL_VALVE", "\u751f\u7269\u8865\u6c34\u9600", 1, 7, 2, 0);
        this.registerReadOnlyValveDevice("DEO_BIO_DRAIN_VALVE", "\u751f\u7269\u6392\u6c34\u9600", 2, 1, 2, 2);
        this.registerIntInputParameter("DEO_PREWASH_PH_AIW16", "\u9884\u6d17PH\u503c", 16);
        this.registerIntInputParameter("DEO_BIO_PH_AIW18", "\u751f\u7269PH\u503c", 18);
        this.registerIntInputParameter("DEO_PREWASH_LEVEL_AIW20", "\u9884\u6d17\u6db2\u4f4d", 20);
        this.registerIntInputParameter("DEO_BIO_LEVEL_AIW22", "\u751f\u7269\u6db2\u4f4d", 22);
        this.registerIntInputParameter("DEO_PREWASH_TEMP_AIW24", "\u9884\u6d17\u6e29\u5ea6", 24);
        this.registerIntInputParameter("DEO_BIO_TEMP_AIW26", "\u751f\u7269\u6e29\u5ea6", 26);
        this.registerIntInputParameter("DEO_WIND_PRESSURE_AIW28", "\u98ce\u538b\u503c", 28);
        this.registerIntInputParameter("DEO_FAN_FREQ_AIW30", "\u98ce\u673a\u9891\u7387", 30);
        this.registerIntInputParameter("DEO_H2S_AIW32", "\u786b\u5316\u6c22", 32);
        this.registerIntInputParameter("DEO_NH3_AIW34", "\u6c28\u6c14", 34);
        this.registerIntInputParameter("DEO_PREWASH_FLOW_AIW36", "\u9884\u6d17\u6d41\u91cf", 36);
        this.registerIntInputParameter("DEO_BIO_FLOW_AIW38", "\u751f\u7269\u6d41\u91cf", 38);
        this.registerBoolInputParameter("DEO_ESTOP_I0_0", "\u6025\u505c", 0, 0);
        this.registerBoolInputParameter("DEO_FAN_RUNNING_I0_1", "\u98ce\u673a\u8fd0\u884c", 0, 1);
        this.registerBoolInputParameter("DEO_FAN_FAULT_I0_2", "\u98ce\u673a\u6545\u969c", 0, 2);
        this.registerBoolInputParameter("DEO_PREWASH_1_RUNNING_I0_3", "\u9884\u6d17\u4e00\u8fd0\u884c", 0, 3);
        this.registerBoolInputParameter("DEO_PREWASH_1_FAULT_I0_4", "\u9884\u6d17\u4e00\u6545\u969c", 0, 4);
        this.registerBoolInputParameter("DEO_PREWASH_2_RUNNING_I0_5", "\u9884\u6d17\u4e8c\u8fd0\u884c", 0, 5);
        this.registerBoolInputParameter("DEO_PREWASH_2_FAULT_I0_6", "\u9884\u6d17\u4e8c\u6545\u969c", 0, 6);
        this.registerBoolInputParameter("DEO_BIO_PUMP_RUNNING_I0_7", "\u751f\u7269\u6cf5\u8fd0\u884c", 0, 7);
        this.registerBoolInputParameter("DEO_BIO_PUMP_FAULT_I1_0", "\u751f\u7269\u6cf5\u6545\u969c", 1, 0);
        this.registerBoolInputParameter("DEO_PREWASH_HEATER_RUNNING_I1_1", "\u9884\u6d17\u7535\u52a0\u70ed\u8fd0\u884c", 1, 1);
        this.registerBoolInputParameter("DEO_BIO_HEATER_RUNNING_I1_2", "\u751f\u7269\u7535\u52a0\u70ed\u8fd0\u884c", 1, 2);
        this.registerBoolInputParameter("DEO_PREWASH_FILL_OPEN_FB_I1_3", "\u9884\u6d17\u8865\u6c34\u9600\u5f00\u5230\u4f4d", 1, 3);
        this.registerBoolInputParameter("DEO_PREWASH_FILL_CLOSE_FB_I1_4", "\u9884\u6d17\u8865\u6c34\u9600\u5173\u5230\u4f4d", 1, 4);
        this.registerBoolInputParameter("DEO_PREWASH_DRAIN_OPEN_FB_I1_5", "\u9884\u6d17\u6392\u6c34\u9600\u5f00\u5230\u4f4d", 1, 5);
        this.registerBoolInputParameter("DEO_PREWASH_DRAIN_CLOSE_FB_I1_6", "\u9884\u6d17\u6392\u6c34\u9600\u5173\u5230\u4f4d", 1, 6);
        this.registerBoolInputParameter("DEO_BIO_FILL_OPEN_FB_I1_7", "\u751f\u7269\u8865\u6c34\u9600\u5f00\u5230\u4f4d", 1, 7);
        this.registerBoolInputParameter("DEO_BIO_FILL_CLOSE_FB_I2_0", "\u751f\u7269\u8865\u6c34\u9600\u5173\u5230\u4f4d", 2, 0);
        this.registerBoolInputParameter("DEO_BIO_DRAIN_OPEN_FB_I2_1", "\u751f\u7269\u6392\u6c34\u9600\u5f00\u5230\u4f4d", 2, 1);
        this.registerBoolInputParameter("DEO_BIO_DRAIN_CLOSE_FB_I2_2", "\u751f\u7269\u6392\u6c34\u9600\u5173\u5230\u4f4d", 2, 2);
        this.registerBoolOutputParameter("DEO_FAN_Q0_0", "\u98ce\u673a\u542f\u505c", 0, 0, false);
        this.registerBoolOutputParameter("DEO_PREWASH_PUMP_1_Q0_1", "\u9884\u6d17\u4e00\u6cf5\u542f\u505c", 0, 1, false);
        this.registerBoolOutputParameter("DEO_PREWASH_PUMP_2_Q0_2", "\u9884\u6d17\u4e8c\u6cf5\u542f\u505c", 0, 2, false);
        this.registerBoolOutputParameter("DEO_BIO_PUMP_Q0_3", "\u751f\u7269\u6cf5\u542f\u505c", 0, 3, false);
        this.registerBoolOutputParameter("DEO_PREWASH_HEATER_Q0_4", "\u9884\u6d17\u7535\u52a0\u70ed\u542f\u505c", 0, 4, false);
        this.registerBoolOutputParameter("DEO_BIO_HEATER_Q0_5", "\u751f\u7269\u7535\u52a0\u70ed\u542f\u505c", 0, 5, false);
        this.registerBoolOutputParameter("DEO_PREWASH_FILL_OPEN_Q0_6", "\u9884\u6d17\u8865\u6c34\u9600\u5f00", 0, 6, false);
        this.registerBoolOutputParameter("DEO_PREWASH_FILL_CLOSE_Q0_7", "\u9884\u6d17\u8865\u6c34\u9600\u5173", 0, 7, false);
        this.registerBoolOutputParameter("DEO_PREWASH_DRAIN_OPEN_Q1_0", "\u9884\u6d17\u6392\u6c34\u9600\u5f00", 1, 0, false);
        this.registerBoolOutputParameter("DEO_PREWASH_DRAIN_CLOSE_Q1_1", "\u9884\u6d17\u6392\u6c34\u9600\u5173", 1, 1, false);
        this.registerBoolOutputParameter("DEO_BIO_FILL_OPEN_Q1_2", "\u751f\u7269\u8865\u6c34\u9600\u5f00", 1, 2, false);
        this.registerBoolOutputParameter("DEO_BIO_FILL_CLOSE_Q1_3", "\u751f\u7269\u8865\u6c34\u9600\u5173", 1, 3, false);
        this.registerBoolOutputParameter("DEO_BIO_DRAIN_OPEN_Q1_4", "\u751f\u7269\u6392\u6c34\u9600\u5f00", 1, 4, false);
        this.registerBoolOutputParameter("DEO_BIO_DRAIN_CLOSE_Q1_5", "\u751f\u7269\u6392\u6c34\u9600\u5173", 1, 5, false);
        this.registerBoolOutputParameter("DEO_GENERAL_FAULT_Q1_6", "\u7efc\u5408\u6545\u969c", 1, 6, false);
        this.registerBoolVParameter("DEO_FAN_START_V3_4", "\u98ce\u673a\u542f\u52a8", 3, 4, true);
        this.registerBoolVParameter("DEO_FAN_STOP_V3_5", "\u98ce\u673a\u505c\u6b62", 3, 5, true);
        this.registerBoolVParameter("DEO_PREWASH_PUMP_1_STOP_V0_3", "\u9884\u6d17\u4e00\u6cf5\u505c\u6b62", 0, 3, true);
        this.registerBoolVParameter("DEO_PREWASH_PUMP_2_START_V0_4", "\u9884\u6d17\u4e8c\u6cf5\u542f\u52a8", 0, 4, true);
        this.registerBoolVParameter("DEO_PREWASH_PUMP_2_STOP_V0_5", "\u9884\u6d17\u4e8c\u6cf5\u505c\u6b62", 0, 5, true);
        this.registerBoolVParameter("DEO_HUMIDIFIER_MANUAL_V9_6", "\u52a0\u6e7f\u6cf5\u624b\u52a8", 9, 6, true);
        this.registerBoolVParameter("DEO_HUMIDIFIER_MODE_SWITCH_V20_0", "\u52a0\u6e7f\u6cf5\u624b\u81ea\u52a8\u8f6c\u6362", 20, 0, true);
        this.registerBoolVParameter("DEO_HUMIDIFIER_RESET_V1000_6", "\u52a0\u6e7f\u6cf5\u6545\u969c\u590d\u4f4d", 1000, 6, true);
        this.registerIntVParameter("DEO_HUMIDIFIER_AUTO_ON_TIME_VW38", "\u52a0\u6e7f\u6cf5\u81ea\u52a8\u5f00\u65f6\u95f4(\u79d2)", 38, true);
        this.registerIntVParameter("DEO_HUMIDIFIER_AUTO_OFF_TIME_VW36", "\u52a0\u6e7f\u6cf5\u81ea\u52a8\u5173\u65f6\u95f4(\u5206)", 36, true);
        this.registerIntVParameter("DEO_HUMIDIFIER_STOP_TIME_DISPLAY_VW3000", "\u52a0\u6e7f\u6cf5\u505c\u6b62\u65f6\u95f4\u663e\u793a", 3000, false);
        this.registerBoolVParameter("DEO_HUMIDIFIER_FAULT_V1000_0", "\u52a0\u6e7f\u6cf5\u6545\u969c", 1000, 0, false);
        this.registerRealVParameter("DEO_BIO_HEATER_AUTO_STOP_VD488", "\u751f\u7269\u81ea\u52a8\u52a0\u70ed\u505c\u6b62", 488, true);
        this.registerRealVParameter("DEO_BIO_HEATER_AUTO_START_VD484", "\u751f\u7269\u81ea\u52a8\u52a0\u70ed\u542f\u52a8", 484, true);
        this.registerBoolVParameter("DEO_BIO_HEATER_MANUAL_STOP_V4_6", "\u751f\u7269\u52a0\u70ed\u624b\u52a8\u542f\u52a8\u505c\u6b62", 4, 6, true);
        this.registerBoolVParameter("DEO_BIO_HEATER_MANUAL_START_V4_5", "\u751f\u7269\u52a0\u70ed\u624b\u52a8\u542f\u52a8\u6309\u94ae", 4, 5, true);
        this.registerBoolVParameter("DEO_BIO_HEATER_MODE_SWITCH_V4_4", "\u751f\u7269\u52a0\u70ed\u624b\u81ea\u52a8\u8f6c\u6362", 4, 4, true);
        this.registerRealVParameter("DEO_PREWASH_HEATER_AUTO_STOP_VD480", "\u9884\u6d17\u81ea\u52a8\u52a0\u70ed\u505c\u6b62", 480, true);
        this.registerRealVParameter("DEO_PREWASH_HEATER_AUTO_START_VD476", "\u9884\u6d17\u81ea\u52a8\u52a0\u70ed\u542f\u52a8", 476, true);
        this.registerBoolVParameter("DEO_PREWASH_HEATER_MANUAL_STOP_V4_2", "\u9884\u6d17\u52a0\u70ed\u624b\u52a8\u505c\u6b62\u6309\u94ae", 4, 2, true);
        this.registerBoolVParameter("DEO_PREWASH_HEATER_MANUAL_START_V4_1", "\u9884\u6d17\u52a0\u70ed\u624b\u52a8\u542f\u52a8\u6309\u94ae", 4, 1, true);
        this.registerBoolVParameter("DEO_PREWASH_HEATER_MODE_SWITCH_V4_0", "\u9884\u6d17\u52a0\u70ed\u624b\u81ea\u52a8\u8f6c\u6362", 4, 0, true);
        this.registerBoolVParameter("DEO_PREWASH_FILL_OPEN_V6_1", "\u9884\u6d17\u8865\u6c34\u9600\u5f00", 6, 1, true);
        this.registerBoolVParameter("DEO_PREWASH_FILL_CLOSE_V6_4", "\u9884\u6d17\u8865\u6c34\u9600\u5173", 6, 4, true);
        this.registerBoolVParameter("DEO_PREWASH_FILL_STOP_V6_2", "\u9884\u6d17\u8865\u6c34\u9600\u505c", 6, 2, true);
        this.registerBoolVParameter("DEO_PREWASH_FILL_MODE_SWITCH_V6_0", "\u9884\u6d17\u8865\u6c34\u9600\u624b\u81ea\u52a8\u8f6c\u6362", 6, 0, true);
        this.registerRealVParameter("DEO_PREWASH_FILL_OPEN_SET_VD492", "\u9884\u6d17\u8865\u6c34\u9600\u5f00\u8bbe\u5b9a", 492, true);
        this.registerRealVParameter("DEO_PREWASH_FILL_CLOSE_SET_VD496", "\u9884\u6d17\u8865\u6c34\u9600\u5173\u8bbe\u5b9a", 496, true);
        this.registerBoolVParameter("DEO_PREWASH_DRAIN_OPEN_V7_1", "\u9884\u6d17\u6392\u6c34\u9600\u5f00", 7, 1, true);
        this.registerBoolVParameter("DEO_PREWASH_DRAIN_CLOSE_V7_4", "\u9884\u6d17\u6392\u6c34\u9600\u5173", 7, 4, true);
        this.registerBoolVParameter("DEO_PREWASH_DRAIN_STOP_V7_2", "\u9884\u6d17\u6392\u6c34\u9600\u505c", 7, 2, true);
        this.registerBoolVParameter("DEO_PREWASH_DRAIN_MODE_SWITCH_V7_0", "\u9884\u6d17\u6392\u6c34\u9600\u624b\u81ea\u52a8\u8f6c\u6362", 7, 0, true);
        this.registerRealVParameter("DEO_PREWASH_DRAIN_OPEN_SET_VD500", "\u9884\u6d17\u6392\u6c34\u9600\u5f00\u8bbe\u5b9a", 500, true);
        this.registerRealVParameter("DEO_PREWASH_DRAIN_CLOSE_SET_VD504", "\u9884\u6d17\u6392\u6c34\u9600\u5173\u8bbe\u5b9a", 504, true);
        this.registerBoolVParameter("DEO_BIO_FILL_OPEN_V8_1", "\u751f\u7269\u8865\u6c34\u9600\u5f00", 8, 1, true);
        this.registerBoolVParameter("DEO_BIO_FILL_CLOSE_V8_4", "\u751f\u7269\u8865\u6c34\u9600\u5173", 8, 4, true);
        this.registerBoolVParameter("DEO_BIO_FILL_STOP_V8_2", "\u751f\u7269\u8865\u6c34\u9600\u505c", 8, 2, true);
        this.registerBoolVParameter("DEO_BIO_FILL_MODE_SWITCH_V8_0", "\u751f\u7269\u8865\u6c34\u9600\u624b\u81ea\u52a8\u8f6c\u6362", 8, 0, true);
        this.registerRealVParameter("DEO_BIO_FILL_OPEN_SET_VD508", "\u751f\u7269\u8865\u6c34\u9600\u5f00\u8bbe\u5b9a", 508, true);
        this.registerRealVParameter("DEO_BIO_FILL_CLOSE_SET_VD512", "\u751f\u7269\u8865\u6c34\u9600\u5173\u8bbe\u5b9a", 512, true);
        this.registerBoolVParameter("DEO_BIO_DRAIN_OPEN_V10_1", "\u751f\u7269\u6392\u6c34\u9600\u5f00", 10, 1, true);
        this.registerBoolVParameter("DEO_BIO_DRAIN_CLOSE_V10_4", "\u751f\u7269\u6392\u6c34\u9600\u5173", 10, 4, true);
        this.registerBoolVParameter("DEO_BIO_DRAIN_STOP_V10_2", "\u751f\u7269\u6392\u6c34\u9600\u505c", 10, 2, true);
        this.registerBoolVParameter("DEO_BIO_DRAIN_MODE_SWITCH_V10_0", "\u751f\u7269\u6392\u6c34\u9600\u624b\u81ea\u52a8\u8f6c\u6362", 10, 0, true);
        this.registerRealVParameter("DEO_BIO_DRAIN_OPEN_SET_VD516", "\u751f\u7269\u6392\u6c34\u9600\u5f00\u8bbe\u5b9a", 516, true);
        this.registerRealVParameter("DEO_BIO_DRAIN_CLOSE_SET_VD520", "\u751f\u7269\u6392\u6c34\u9600\u5173\u8bbe\u5b9a", 520, true);
        this.registerRealVParameter("DEO_PREWASH_PH_VD300", "\u9884\u6d17PH\u503c", 300, false);
        this.registerRealVParameter("DEO_BIO_PH_VD304", "\u751f\u7269PH\u503c", 304, false);
        this.registerRealVParameter("DEO_PREWASH_LEVEL_VD308", "\u9884\u6d17\u6db2\u4f4d", 308, false);
        this.registerRealVParameter("DEO_PREWASH_LEVEL_HIGH_VD400", "\u9884\u6d17\u6db2\u4f4d\u4e0a\u9650", 400, true);
        this.registerRealVParameter("DEO_PREWASH_LEVEL_LOW_VD404", "\u9884\u6d17\u6db2\u4f4d\u4e0b\u9650", 404, true);
        this.registerRealVParameter("DEO_BIO_LEVEL_VD312", "\u751f\u7269\u6db2\u4f4d", 312, false);
        this.registerRealVParameter("DEO_BIO_LEVEL_HIGH_VD408", "\u751f\u7269\u6db2\u4f4d\u4e0a\u9650", 408, true);
        this.registerRealVParameter("DEO_BIO_LEVEL_LOW_VD412", "\u751f\u7269\u6db2\u4f4d\u4e0b\u9650", 412, true);
        this.registerRealVParameter("DEO_PREWASH_TEMP_VD316", "\u9884\u6d17\u6e29\u5ea6", 316, false);
        this.registerRealVParameter("DEO_PREWASH_TEMP_HIGH_VD416", "\u9884\u6d17\u6e29\u5ea6\u4e0a\u9650", 416, true);
        this.registerRealVParameter("DEO_PREWASH_TEMP_LOW_VD420", "\u9884\u6d17\u6e29\u5ea6\u4e0b\u9650", 420, true);
        this.registerRealVParameter("DEO_BIO_TEMP_VD320", "\u751f\u7269\u6e29\u5ea6", 320, false);
        this.registerRealVParameter("DEO_BIO_TEMP_HIGH_VD424", "\u751f\u7269\u6e29\u5ea6\u4e0a\u9650", 424, true);
        this.registerRealVParameter("DEO_BIO_TEMP_LOW_VD428", "\u751f\u7269\u6e29\u5ea6\u4e0b\u9650", 428, true);
        this.registerRealVParameter("DEO_WIND_PRESSURE_VD324", "\u98ce\u538b\u503c", 324, false);
        this.registerRealVParameter("DEO_WIND_PRESSURE_HIGH_VD432", "\u98ce\u538b\u503c\u4e0a\u9650", 432, true);
        this.registerRealVParameter("DEO_WIND_PRESSURE_LOW_VD436", "\u98ce\u538b\u503c\u4e0b\u9650", 436, true);
        this.registerRealVParameter("DEO_FAN_FREQ_VD328", "\u98ce\u673a\u9891\u7387", 328, false);
        this.registerRealVParameter("DEO_H2S_VD332", "\u786b\u5316\u6c22\u503c", 332, false);
        this.registerRealVParameter("DEO_H2S_HIGH_VD440", "\u786b\u5316\u6c22\u4e0a\u9650", 440, true);
        this.registerRealVParameter("DEO_H2S_LOW_VD444", "\u786b\u5316\u6c22\u4e0b\u9650", 444, true);
        this.registerRealVParameter("DEO_NH3_VD336", "\u6c28\u6c14\u503c", 336, false);
        this.registerRealVParameter("DEO_NH3_HIGH_VD448", "\u6c28\u6c14\u4e0a\u9650", 448, true);
        this.registerRealVParameter("DEO_NH3_LOW_VD452", "\u6c28\u6c14\u4e0b\u9650", 452, true);
        this.registerRealVParameter("DEO_PREWASH_FLOW_VD340", "\u9884\u6d17\u6d41\u91cf\u503c", 340, false);
        this.registerRealVParameter("DEO_PREWASH_FLOW_HIGH_VD456", "\u9884\u6d17\u6d41\u91cf\u4e0a\u9650", 456, true);
        this.registerRealVParameter("DEO_PREWASH_FLOW_LOW_VD460", "\u9884\u6d17\u6d41\u91cf\u4e0b\u9650", 460, true);
        this.registerRealVParameter("DEO_BIO_FLOW_VD344", "\u9884\u751f\u7269\u6d41\u91cf\u503c", 344, false);
        this.registerRealVParameter("DEO_BIO_FLOW_HIGH_VD464", "\u751f\u7269\u6d41\u91cf\u4e0a\u9650", 464, true);
        this.registerRealVParameter("DEO_BIO_FLOW_LOW_VD468", "\u751f\u7269\u6d41\u91cf\u4e0b\u9650", 468, true);
        this.registerRealVParameter("DEO_FAN_FREQ_SET_VD472", "\u98ce\u673a\u9891\u7387\u8bbe\u5b9a", 472, true);
        this.registerBoolVParameter("DEO_SYSTEM_ONE_KEY_START_STOP_V2_5", "\u9664\u81ed\u7cfb\u7edf\u4e00\u952e\u542f\u505c", 2, 5, true);
        this.registerBoolVParameter("DEO_SYSTEM_MODE_SWITCH_V0_2", "\u9664\u81ed\u7cfb\u7edf\u624b\u81ea\u52a8\u8f6c\u6362", 0, 2, true);
        this.registerBoolVParameter("DEO_TOUCHSCREEN_ESTOP_V0_1", "\u89e6\u6478\u5c4f\u6025\u505c", 0, 1, true);
        this.registerBoolVParameter("DEO_ESTOP_INDICATOR_V0_0", "\u6025\u505c\u706f", 0, 0, false);
        this.registerIntVParameter("DEO_SYSTEM_START_DELAY_VW530", "\u4e00\u952e\u542f\u52a8\u5ef6\u65f6\u65f6\u95f4", 530, true);
        this.registerIntVParameter("DEO_SYSTEM_STOP_DELAY_VW532", "\u4e00\u952e\u505c\u6b62\u5ef6\u65f6\u65f6\u95f4", 532, true);
        this.registerBoolMParameter("DEO_TOUCHSCREEN_ESTOP_M0_1", "\u89e6\u6478\u5c4f\u6025\u505c", 0, 1, true);
        this.registerBoolMParameter("DEO_ESTOP_INDICATOR_M0_0", "\u6025\u505c\u6307\u793a", 0, 0, false);
    }

    @Override
    public boolean writeParameter(String string, String string2) {
        String string3;
        String string4 = string3 = string == null ? null : string.toUpperCase();
        if (!PULSE_PARAMETERS.contains(string3)) {
            if (TOGGLE_PARAMETERS.contains(string3)) {
                String string5 = string3 + "_TAG";
                Map<String, Object> map = this.readTags(Collections.singletonList(string5));
                Object object = map.get(string5);
                if (!(object instanceof Boolean)) {
                    throw new IllegalStateException("Unable to read BOOL toggle parameter: " + string3);
                }
                return this.writeBoolTag(string5, (Boolean)object == false);
            }
            return super.writeParameter(string, string2);
        }
        String string6 = string3 + "_TAG";
        boolean bl = "1".equals(string2) || "true".equalsIgnoreCase(string2);
        return !bl ? this.writeBoolTag(string6, false) : this.pulseBoolTag(string6, 300L);
    }

    private void registerReadOnlyMotorDevice(String string, String string2, int n, int n2, Integer n3, Integer n4) {
        String string3 = this.registerBoolInputTag(string + "_RUNNING_TAG", string2 + "\u8fd0\u884c", n, n2, false);
        String string4 = n3 == null ? null : this.registerBoolInputTag(string + "_FAULT_TAG", string2 + "\u6545\u969c", n3, n4, false);
        this.registerDevice(string, string2, "motor", string3, null, null, string4);
    }

    private void registerReadOnlyValveDevice(String string, String string2, int n, int n2, int n3, int n4) {
        String string3 = this.registerBoolInputTag(string + "_OPEN_FB_TAG", string2 + "\u5f00\u5230\u4f4d", n, n2, false);
        String string4 = this.registerBoolInputTag(string + "_CLOSE_FB_TAG", string2 + "\u5173\u5230\u4f4d", n3, n4, false);
        this.registerDevice(string, string2, "valve", null, null, null, null, string3, string4);
    }

    private void registerIntInputParameter(String string, String string2, int n) {
        this.registerParameter(string, string2, this.registerIntInputWordTag(string + "_TAG", string2, n, false));
    }

    private void registerBoolInputParameter(String string, String string2, int n, int n2) {
        this.registerParameter(string, string2, this.registerBoolInputTag(string + "_TAG", string2, n, n2, false));
    }

    private void registerBoolOutputParameter(String string, String string2, int n, int n2, boolean bl) {
        this.registerParameter(string, string2, this.registerBoolOutputTag(string + "_TAG", string2, n, n2, bl));
    }

    private void registerBoolVParameter(String string, String string2, int n, int n2, boolean bl) {
        this.registerParameter(string, string2, this.registerBoolVTag(string + "_TAG", string2, n, n2, bl));
    }

    private void registerBoolMParameter(String string, String string2, int n, int n2, boolean bl) {
        this.registerParameter(string, string2, this.registerBoolMTag(string + "_TAG", string2, n, n2, bl));
    }

    private void registerIntVParameter(String string, String string2, int n, boolean bl) {
        this.registerParameter(string, string2, this.registerIntVTag(string + "_TAG", string2, n, bl));
    }

    private void registerRealVParameter(String string, String string2, int n, boolean bl) {
        this.registerParameter(string, string2, this.registerRealVTag(string + "_TAG", string2, n, bl));
    }
}

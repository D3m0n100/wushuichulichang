/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractS7200SmartPlcService;
import com.example.waterfactoryceshi.service.FanPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FanPlcService
extends AbstractS7200SmartPlcService {
    public FanPlcService(FanPlcConnectionPool connectionPool, @Value(value="${plc.fan.ip}") String plcIp, @Value(value="${plc.fan.rack:0}") int rack, @Value(value="${plc.fan.slot:1}") int slot) {
        super(connectionPool, "Fan", "S7-200 SMART SR40/SR20", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerDevice("FAN_SYS_1", "1#\u98ce\u673a\u7cfb\u7edf", "fan-system", this.registerBoolVTag("FAN_SYS_1_RUNNING", "1#\u8fd0\u884c\u4e2d", 3004, 0, false), this.registerBoolVTag("FAN_SYS_1_CMD", "1#\u7cfb\u7edf\u542f\u505c(\u4fdd\u6301)", 3000, 0, true), null, this.registerBoolVTag("FAN_SYS_1_FAULT", "1#\u6545\u969c\u62a5\u8b66", 3004, 2, false));
        this.registerDevice("FAN_SYS_2", "2#\u98ce\u673a\u7cfb\u7edf", "fan-system", this.registerBoolVTag("FAN_SYS_2_RUNNING", "2#\u8fd0\u884c\u4e2d", 3006, 0, false), this.registerBoolVTag("FAN_SYS_2_CMD", "2#\u7cfb\u7edf\u542f\u505c(\u4fdd\u6301)", 3000, 1, true), null, this.registerBoolVTag("FAN_SYS_2_FAULT", "2#\u6545\u969c\u62a5\u8b66", 3006, 2, false));
        this.registerDevice("FAN_SYS_3", "3#\u98ce\u673a\u7cfb\u7edf", "fan-system", this.registerBoolVTag("FAN_SYS_3_RUNNING", "3#\u8fd0\u884c\u4e2d", 3008, 0, false), this.registerBoolVTag("FAN_SYS_3_CMD", "3#\u7cfb\u7edf\u542f\u505c(\u4fdd\u6301)", 3000, 2, true), null, this.registerBoolVTag("FAN_SYS_3_FAULT", "3#\u6545\u969c\u62a5\u8b66", 3008, 2, false));
        this.registerParameter("FAN_SYS_1_ENABLE", "1#\u98ce\u673a\u7cfb\u7edf\u542f\u505c(\u4fdd\u6301)", "FAN_SYS_1_CMD");
        this.registerParameter("FAN_SYS_2_ENABLE", "2#\u98ce\u673a\u7cfb\u7edf\u542f\u505c(\u4fdd\u6301)", "FAN_SYS_2_CMD");
        this.registerParameter("FAN_SYS_3_ENABLE", "3#\u98ce\u673a\u7cfb\u7edf\u542f\u505c(\u4fdd\u6301)", "FAN_SYS_3_CMD");
        this.registerParameter("FAN_SYS_1_RESET", "1#\u6545\u969c\u590d\u4f4d(\u4fdd\u6301)", this.registerBoolVTag("FAN_SYS_1_RESET_TAG", "1#\u6545\u969c\u590d\u4f4d(\u4fdd\u6301)", 3000, 3, true));
        this.registerParameter("FAN_SYS_2_RESET", "2#\u6545\u969c\u590d\u4f4d(\u4fdd\u6301)", this.registerBoolVTag("FAN_SYS_2_RESET_TAG", "2#\u6545\u969c\u590d\u4f4d(\u4fdd\u6301)", 3000, 4, true));
        this.registerParameter("FAN_SYS_3_RESET", "3#\u6545\u969c\u590d\u4f4d(\u4fdd\u6301)", this.registerBoolVTag("FAN_SYS_3_RESET_TAG", "3#\u6545\u969c\u590d\u4f4d(\u4fdd\u6301)", 3000, 5, true));
        this.registerParameter("FAN_SYS_1_TARGET_FREQ", "1#\u76ee\u6807\u9891\u7387", this.registerRealVTag("FAN_SYS_1_TARGET_FREQ_TAG", "1#\u76ee\u6807\u9891\u7387", 3018, true));
        this.registerParameter("FAN_SYS_2_TARGET_FREQ", "2#\u76ee\u6807\u9891\u7387", this.registerRealVTag("FAN_SYS_2_TARGET_FREQ_TAG", "2#\u76ee\u6807\u9891\u7387", 3022, true));
        this.registerParameter("FAN_SYS_3_TARGET_FREQ", "3#\u76ee\u6807\u9891\u7387", this.registerRealVTag("FAN_SYS_3_TARGET_FREQ_TAG", "3#\u76ee\u6807\u9891\u7387", 3026, true));
        this.registerParameter("FAN_SYS_1_INVERTER_FAULT", "1#\u53d8\u9891\u5668\u6545\u969c", this.registerBoolVTag("FAN_SYS_1_INVERTER_FAULT_TAG", "1#\u53d8\u9891\u5668\u6545\u969c", 3004, 3, false));
        this.registerParameter("FAN_SYS_1_COOLING_FAULT", "1#\u9694\u97f3\u7f69\u6563\u70ed\u6545\u969c", this.registerBoolVTag("FAN_SYS_1_COOLING_FAULT_TAG", "1#\u9694\u97f3\u7f69\u6563\u70ed\u6545\u969c", 3004, 4, false));
        this.registerParameter("FAN_SYS_1_INLET_PRESSURE_ALARM", "1#\u8fdb\u53e3\u538b\u529b\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_1_INLET_PRESSURE_ALARM_TAG", "1#\u8fdb\u53e3\u538b\u529b\u62a5\u8b66", 3004, 5, false));
        this.registerParameter("FAN_SYS_1_OUTLET_PRESSURE_ALARM", "1#\u51fa\u53e3\u538b\u529b\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_1_OUTLET_PRESSURE_ALARM_TAG", "1#\u51fa\u53e3\u538b\u529b\u62a5\u8b66", 3004, 6, false));
        this.registerParameter("FAN_SYS_1_TEMP_ALARM", "1#\u6e29\u5ea6\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_1_TEMP_ALARM_TAG", "1#\u6e29\u5ea6\u62a5\u8b66", 3004, 7, false));
        this.registerParameter("FAN_SYS_1_CURRENT_ALARM", "1#\u7535\u6d41\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_1_CURRENT_ALARM_TAG", "1#\u7535\u6d41\u62a5\u8b66", 3005, 0, false));
        this.registerParameter("FAN_SYS_2_INVERTER_FAULT", "2#\u53d8\u9891\u5668\u6545\u969c", this.registerBoolVTag("FAN_SYS_2_INVERTER_FAULT_TAG", "2#\u53d8\u9891\u5668\u6545\u969c", 3006, 3, false));
        this.registerParameter("FAN_SYS_2_COOLING_FAULT", "2#\u9694\u97f3\u7f69\u6563\u70ed\u6545\u969c", this.registerBoolVTag("FAN_SYS_2_COOLING_FAULT_TAG", "2#\u9694\u97f3\u7f69\u6563\u70ed\u6545\u969c", 3006, 4, false));
        this.registerParameter("FAN_SYS_2_INLET_PRESSURE_ALARM", "2#\u8fdb\u53e3\u538b\u529b\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_2_INLET_PRESSURE_ALARM_TAG", "2#\u8fdb\u53e3\u538b\u529b\u62a5\u8b66", 3006, 5, false));
        this.registerParameter("FAN_SYS_2_OUTLET_PRESSURE_ALARM", "2#\u51fa\u53e3\u538b\u529b\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_2_OUTLET_PRESSURE_ALARM_TAG", "2#\u51fa\u53e3\u538b\u529b\u62a5\u8b66", 3006, 6, false));
        this.registerParameter("FAN_SYS_2_TEMP_ALARM", "2#\u6e29\u5ea6\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_2_TEMP_ALARM_TAG", "2#\u6e29\u5ea6\u62a5\u8b66", 3006, 7, false));
        this.registerParameter("FAN_SYS_2_CURRENT_ALARM", "2#\u7535\u6d41\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_2_CURRENT_ALARM_TAG", "2#\u7535\u6d41\u62a5\u8b66", 3007, 0, false));
        this.registerParameter("FAN_SYS_3_INVERTER_FAULT", "3#\u53d8\u9891\u5668\u6545\u969c", this.registerBoolVTag("FAN_SYS_3_INVERTER_FAULT_TAG", "3#\u53d8\u9891\u5668\u6545\u969c", 3008, 3, false));
        this.registerParameter("FAN_SYS_3_COOLING_FAULT", "3#\u9694\u97f3\u7f69\u6563\u70ed\u6545\u969c", this.registerBoolVTag("FAN_SYS_3_COOLING_FAULT_TAG", "3#\u9694\u97f3\u7f69\u6563\u70ed\u6545\u969c", 3008, 4, false));
        this.registerParameter("FAN_SYS_3_INLET_PRESSURE_ALARM", "3#\u8fdb\u53e3\u538b\u529b\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_3_INLET_PRESSURE_ALARM_TAG", "3#\u8fdb\u53e3\u538b\u529b\u62a5\u8b66", 3008, 5, false));
        this.registerParameter("FAN_SYS_3_OUTLET_PRESSURE_ALARM", "3#\u51fa\u53e3\u538b\u529b\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_3_OUTLET_PRESSURE_ALARM_TAG", "3#\u51fa\u53e3\u538b\u529b\u62a5\u8b66", 3008, 6, false));
        this.registerParameter("FAN_SYS_3_TEMP_ALARM", "3#\u6e29\u5ea6\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_3_TEMP_ALARM_TAG", "3#\u6e29\u5ea6\u62a5\u8b66", 3008, 7, false));
        this.registerParameter("FAN_SYS_3_CURRENT_ALARM", "3#\u7535\u6d41\u62a5\u8b66", this.registerBoolVTag("FAN_SYS_3_CURRENT_ALARM_TAG", "3#\u7535\u6d41\u62a5\u8b66", 3009, 0, false));
        this.registerTelemetry("FAN_SYS_1", 3058);
        this.registerTelemetry("FAN_SYS_2", 3118);
        this.registerTelemetry("FAN_SYS_3", 3178);
    }

    public boolean executeCommand(String deviceCode, String action, long pulseMs) {
        String normalizedAction;
        String normalizedDeviceCode = deviceCode == null ? "" : deviceCode.toUpperCase();
        String string = normalizedAction = action == null ? "HOLD_ON" : action.toUpperCase();
        if ("HOLD_ON".equals(normalizedAction) || "START_HOLD".equals(normalizedAction) || "ENABLE_ON".equals(normalizedAction) || "PULSE".equals(normalizedAction)) {
            return this.writeSystemEnable(normalizedDeviceCode, true);
        }
        if ("HOLD_OFF".equals(normalizedAction) || "STOP_HOLD".equals(normalizedAction) || "ENABLE_OFF".equals(normalizedAction)) {
            return this.writeSystemEnable(normalizedDeviceCode, false);
        }
        if ("RESET_ON".equals(normalizedAction)) {
            return this.writeReset(normalizedDeviceCode, true);
        }
        if ("RESET_OFF".equals(normalizedAction) || "RESET_PULSE".equals(normalizedAction)) {
            return this.writeReset(normalizedDeviceCode, false);
        }
        throw new IllegalArgumentException("Unsupported fan command action: " + action);
    }

    public boolean writeSystemEnable(String deviceCode, boolean enabled) {
        return this.writeParameter(this.enableParameterCodeFor(deviceCode), String.valueOf(enabled));
    }

    public boolean writeReset(String deviceCode, boolean enabled) {
        return this.writeParameter(this.resetParameterCodeFor(deviceCode), String.valueOf(enabled));
    }

    private String resetParameterCodeFor(String deviceCode) {
        if ("FAN_SYS_1".equals(deviceCode)) {
            return "FAN_SYS_1_RESET";
        }
        if ("FAN_SYS_2".equals(deviceCode)) {
            return "FAN_SYS_2_RESET";
        }
        if ("FAN_SYS_3".equals(deviceCode)) {
            return "FAN_SYS_3_RESET";
        }
        throw new IllegalArgumentException("Unsupported fan reset device: " + deviceCode);
    }

    private String enableParameterCodeFor(String deviceCode) {
        if ("FAN_SYS_1".equals(deviceCode)) {
            return "FAN_SYS_1_ENABLE";
        }
        if ("FAN_SYS_2".equals(deviceCode)) {
            return "FAN_SYS_2_ENABLE";
        }
        if ("FAN_SYS_3".equals(deviceCode)) {
            return "FAN_SYS_3_ENABLE";
        }
        throw new IllegalArgumentException("Unsupported fan enable device: " + deviceCode);
    }

    private void registerTelemetry(String prefix, int baseOffset) {
        this.registerParameter(prefix + "_OUTPUT_FREQ", prefix + "\u8f93\u51fa\u9891\u7387", this.registerRealVTag(prefix + "_OUTPUT_FREQ_TAG", prefix + "\u8f93\u51fa\u9891\u7387", baseOffset, false));
        this.registerParameter(prefix + "_BUS_VOLTAGE", prefix + "\u6bcd\u7ebf\u7535\u538b", this.registerRealVTag(prefix + "_BUS_VOLTAGE_TAG", prefix + "\u6bcd\u7ebf\u7535\u538b", baseOffset + 4, false));
        this.registerParameter(prefix + "_OUTPUT_VOLTAGE", prefix + "\u8f93\u51fa\u7535\u538b", this.registerRealVTag(prefix + "_OUTPUT_VOLTAGE_TAG", prefix + "\u8f93\u51fa\u7535\u538b", baseOffset + 8, false));
        this.registerParameter(prefix + "_OUTPUT_CURRENT", prefix + "\u8f93\u51fa\u7535\u6d41", this.registerRealVTag(prefix + "_OUTPUT_CURRENT_TAG", prefix + "\u8f93\u51fa\u7535\u6d41", baseOffset + 12, false));
        this.registerParameter(prefix + "_OUTPUT_POWER", prefix + "\u8f93\u51fa\u529f\u7387", this.registerRealVTag(prefix + "_OUTPUT_POWER_TAG", prefix + "\u8f93\u51fa\u529f\u7387", baseOffset + 16, false));
        this.registerParameter(prefix + "_OUTPUT_TORQUE", prefix + "\u8f93\u51fa\u8f6c\u77e9", this.registerRealVTag(prefix + "_OUTPUT_TORQUE_TAG", prefix + "\u8f93\u51fa\u8f6c\u77e9", baseOffset + 20, false));
        this.registerParameter(prefix + "_RUN_SPEED", prefix + "\u8fd0\u884c\u901f\u5ea6", this.registerRealVTag(prefix + "_RUN_SPEED_TAG", prefix + "\u8fd0\u884c\u901f\u5ea6", baseOffset + 24, false));
        this.registerParameter(prefix + "_INVERTER_TEMP", prefix + "\u53d8\u9891\u5668\u6e29\u5ea6", this.registerRealVTag(prefix + "_INVERTER_TEMP_TAG", prefix + "\u53d8\u9891\u5668\u6e29\u5ea6", baseOffset + 28, false));
    }
}

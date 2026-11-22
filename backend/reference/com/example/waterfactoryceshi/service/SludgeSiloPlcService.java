/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractS7200SmartPlcService;
import com.example.waterfactoryceshi.service.SludgeSiloPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SludgeSiloPlcService
extends AbstractS7200SmartPlcService {
    public SludgeSiloPlcService(SludgeSiloPlcConnectionPool connectionPool, @Value(value="${plc.sludgeSilo.ip}") String plcIp, @Value(value="${plc.sludgeSilo.rack:0}") int rack, @Value(value="${plc.sludgeSilo.slot:1}") int slot) {
        super(connectionPool, "SludgeSilo", "S7-200 SMART SR20", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerDevice("SLUDGE_GATE", "\u6c61\u6ce5\u6599\u4ed3\u63d2\u677f\u9600", "valve", this.registerBoolVTag("SLUDGE_GATE_RUNNING", "\u63d2\u677f\u9600\u8fd0\u884c\u53cd\u9988", 0, 4, false), null, null, this.registerBoolVTag("SLUDGE_GATE_FAULT", "\u63d2\u677f\u9600\u6545\u969c\u53cd\u9988", 0, 3, false));
        this.registerDevice("SLUDGE_VIBRATOR", "\u6c61\u6ce5\u6599\u4ed3\u9707\u52a8", "vibrator", this.registerBoolVTag("SLUDGE_VIBRATOR_RUNNING", "\u9707\u52a8\u8fd0\u884c\u53cd\u9988", 0, 5, false), null, null, null);
        this.registerParameter("SLUDGE_REMOTE_ALLOWED", "\u63d2\u677f\u9600\u8fdc\u7a0b\u5141\u8bb8", this.registerBoolVTag("SLUDGE_REMOTE_ALLOWED_TAG", "\u63d2\u677f\u9600\u8fdc\u7a0b\u5141\u8bb8", 0, 0, false));
        this.registerParameter("SLUDGE_GATE_OPEN_FEEDBACK", "\u95f8\u677f\u5f00\u53cd\u9988", this.registerBoolVTag("SLUDGE_GATE_OPEN_FEEDBACK_TAG", "\u95f8\u677f\u5f00\u53cd\u9988", 0, 1, false));
        this.registerParameter("SLUDGE_GATE_CLOSE_FEEDBACK", "\u95f8\u677f\u5173\u53cd\u9988", this.registerBoolVTag("SLUDGE_GATE_CLOSE_FEEDBACK_TAG", "\u95f8\u677f\u5173\u53cd\u9988", 0, 2, false));
        this.registerParameter("SLUDGE_GATE_FAULT_FEEDBACK", "\u95f8\u677f\u6545\u969c\u53cd\u9988", this.registerBoolVTag("SLUDGE_GATE_FAULT_FEEDBACK_TAG", "\u95f8\u677f\u6545\u969c\u53cd\u9988", 0, 3, false));
        this.registerParameter("SLUDGE_GATE_RUNNING_FEEDBACK", "\u95f8\u677f\u8fd0\u884c\u53cd\u9988", this.registerBoolVTag("SLUDGE_GATE_RUNNING_FEEDBACK_TAG", "\u95f8\u677f\u8fd0\u884c\u53cd\u9988", 0, 4, false));
        this.registerParameter("SLUDGE_VIBRATION_RUNNING_FEEDBACK", "\u9707\u52a8\u8fd0\u884c\u53cd\u9988", this.registerBoolVTag("SLUDGE_VIBRATION_RUNNING_FEEDBACK_TAG", "\u9707\u52a8\u8fd0\u884c\u53cd\u9988", 0, 5, false));
        this.registerParameter("SLUDGE_LEVEL", "\u4ed3\u6599\u4f4d", this.registerIntVTag("SLUDGE_LEVEL_TAG", "\u4ed3\u6599\u4f4d", 2, false));
        this.registerParameter("SLUDGE_GATE_OPEN_CMD", "\u8fdc\u7a0b\u5f00\u9600", this.registerBoolVTag("SLUDGE_GATE_OPEN_CMD_TAG", "\u8fdc\u7a0b\u5f00\u9600", 10, 0, false));
        this.registerParameter("SLUDGE_GATE_CLOSE_CMD", "\u8fdc\u7a0b\u5173\u9600", this.registerBoolVTag("SLUDGE_GATE_CLOSE_CMD_TAG", "\u8fdc\u7a0b\u5173\u9600", 10, 1, false));
        this.registerParameter("SLUDGE_VIBRATOR_CMD", "\u8fdc\u7a0b\u9707\u52a8", this.registerBoolVTag("SLUDGE_VIBRATOR_CMD_TAG", "\u8fdc\u7a0b\u9707\u52a8", 10, 4, false));
    }
}

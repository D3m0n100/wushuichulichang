/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractWastewaterSmartPlcService;
import com.example.waterfactoryceshi.service.RotorPumpPlcConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RotorPumpPlcService
extends AbstractWastewaterSmartPlcService {
    public RotorPumpPlcService(RotorPumpPlcConnectionPool connectionPool, @Value(value="${plc.rotorPump.ip}") String plcIp, @Value(value="${plc.rotorPump.rack:0}") int rack, @Value(value="${plc.rotorPump.slot:1}") int slot) {
        super(connectionPool, "RotorPump", "S7-200 SMART", plcIp, rack, slot);
    }

    @Override
    protected void initDefinitions() {
        this.registerInputStatusDevice("ROTOR_PUMP", "\u8f6c\u5b50\u6cf5", "pump", 0, 0, 0, 1);
        this.registerInputStatusDevice("CONVEYOR_1", "\u8f93\u9001\u673a1", "conveyor", 0, 2, 0, 3);
        this.registerInputStatusDevice("CONVEYOR_2", "\u8f93\u9001\u673a2", "conveyor", 0, 5, 0, 6);
        this.registerBoolInputParameter("ROTOR_PUMP_RUNNING", "\u8f6c\u5b50\u6cf5\u8fd0\u884c\u4fe1\u53f7", 0, 0);
        this.registerBoolInputParameter("ROTOR_PUMP_FAULT", "\u8f6c\u5b50\u6cf5\u6545\u969c\u4fe1\u53f7", 0, 1);
        this.registerBoolInputParameter("CONVEYOR_1_RUNNING", "\u8f93\u9001\u673a1\u8fd0\u884c\u4fe1\u53f7", 0, 2);
        this.registerBoolInputParameter("CONVEYOR_1_FAULT", "\u8f93\u9001\u673a1\u6545\u969c\u4fe1\u53f7", 0, 3);
        this.registerBoolInputParameter("CONVEYOR_1_REMOTE", "\u8f93\u9001\u673a1\u8fdc\u7a0b\u4fe1\u53f7", 0, 4);
        this.registerBoolInputParameter("CONVEYOR_2_RUNNING", "\u8f93\u9001\u673a2\u8fd0\u884c\u4fe1\u53f7", 0, 5);
        this.registerBoolInputParameter("CONVEYOR_2_FAULT", "\u8f93\u9001\u673a2\u6545\u969c\u4fe1\u53f7", 0, 6);
        this.registerBoolInputParameter("CONVEYOR_2_REMOTE", "\u8f93\u9001\u673a2\u8fdc\u7a0b\u4fe1\u53f7", 0, 7);
    }
}

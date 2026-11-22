/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractS7ConnectionPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PamPlcConnectionPool
extends AbstractS7ConnectionPool {
    public PamPlcConnectionPool(@Value(value="${plc.pam.ip}") String ip, @Value(value="${plc.pam.rack:0}") int rack, @Value(value="${plc.pam.slot:1}") int slot) {
        super(ip, rack, slot);
    }
}

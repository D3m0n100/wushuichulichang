/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.apache.plc4x.java.api.PlcConnection
 */
package com.example.waterfactoryceshi.service;

import org.apache.plc4x.java.api.PlcConnection;

public interface PlcConnectionProvider {
    public PlcConnection borrowConnection() throws Exception;

    public void returnConnection(PlcConnection var1);

    default public int maxReadAttempts() {
        return 1;
    }

    default public void invalidateConnection(PlcConnection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.AbstractS7200SmartPlcService;
import com.example.waterfactoryceshi.service.PlcConnectionProvider;

public abstract class AbstractWastewaterSmartPlcService
extends AbstractS7200SmartPlcService {
    protected AbstractWastewaterSmartPlcService(PlcConnectionProvider connectionProvider, String plcName, String series, String plcIp, int rack, int slot) {
        super(connectionProvider, plcName, series, plcIp, rack, slot);
    }

    protected void registerVAreaStartStopDevice(String code, String name, String type, int controlByteOffset, int stateByteOffset) {
        this.registerDevice(code, name, type, this.registerBoolVTag(code + "_RUNNING_TAG", name + "\u8fd0\u884c", stateByteOffset, 1, false), this.registerBoolVTag(code + "_START_CMD_TAG", name + "\u624b\u52a8\u542f\u52a8", controlByteOffset, 1, true), this.registerBoolVTag(code + "_MANUAL_MODE_TAG", name + "\u624b\u81ea\u52a8", controlByteOffset, 0, true), this.registerBoolVTag(code + "_FAULT_TAG", name + "\u6545\u969c", controlByteOffset, 5, false));
        this.registerParameter(code + "_STOP", name + "\u624b\u52a8\u505c\u6b62", this.registerBoolVTag(code + "_STOP_TAG", name + "\u624b\u52a8\u505c\u6b62", controlByteOffset, 2, true));
        this.registerParameter(code + "_REMOTE", name + "\u8fdc\u7a0b", this.registerBoolVTag(code + "_REMOTE_TAG", name + "\u8fdc\u7a0b", stateByteOffset, 0, false));
    }

    protected void registerInputStatusDevice(String code, String name, String type, int runningByteOffset, int runningBitOffset, Integer faultByteOffset, Integer faultBitOffset) {
        String faultTag = null;
        if (faultByteOffset != null && faultBitOffset != null) {
            faultTag = this.registerBoolInputTag(code + "_FAULT_TAG", name + "\u6545\u969c", faultByteOffset, faultBitOffset, false);
        }
        this.registerDevice(code, name, type, this.registerBoolInputTag(code + "_RUNNING_TAG", name + "\u8fd0\u884c", runningByteOffset, runningBitOffset, false), null, null, faultTag);
    }

    protected void registerRealInstrumentCluster(String prefix, String name, int upperOffset, int lowerOffset, int outputOffset, int highOffset, int midOffset, Integer lowOffset) {
        this.registerParameter(prefix + "_UPPER_LIMIT", name + "\u4e0a\u9650\u503c", this.registerRealVTag(prefix + "_UPPER_LIMIT_TAG", name + "\u4e0a\u9650\u503c", upperOffset, true));
        this.registerParameter(prefix + "_LOWER_LIMIT", name + "\u4e0b\u9650\u503c", this.registerRealVTag(prefix + "_LOWER_LIMIT_TAG", name + "\u4e0b\u9650\u503c", lowerOffset, true));
        this.registerParameter(prefix + "_OUTPUT", name + "\u8f93\u51fa\u503c", this.registerRealVTag(prefix + "_OUTPUT_TAG", name + "\u8f93\u51fa\u503c", outputOffset, false));
        this.registerParameter(prefix + "_HIGH_SET", name + "\u9ad8\u8bbe\u7f6e", this.registerRealVTag(prefix + "_HIGH_SET_TAG", name + "\u9ad8\u8bbe\u7f6e", highOffset, true));
        this.registerParameter(prefix + "_MID_SET", name + "\u4e2d\u8bbe\u7f6e", this.registerRealVTag(prefix + "_MID_SET_TAG", name + "\u4e2d\u8bbe\u7f6e", midOffset, true));
        if (lowOffset != null) {
            this.registerParameter(prefix + "_LOW_SET", name + "\u4f4e\u8bbe\u7f6e", this.registerRealVTag(prefix + "_LOW_SET_TAG", name + "\u4f4e\u8bbe\u7f6e", lowOffset, true));
        }
    }

    protected void registerIntTimerParameter(String code, String name, int byteOffset, boolean writable) {
        this.registerParameter(code, name, this.registerIntVTag(code + "_TAG", name, byteOffset, writable));
    }

    protected void registerBoolVParameter(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        this.registerParameter(code, name, this.registerBoolVTag(code + "_TAG", name, byteOffset, bitOffset, writable));
    }

    protected void registerBoolInputParameter(String code, String name, int byteOffset, int bitOffset) {
        this.registerParameter(code, name, this.registerBoolInputTag(code + "_TAG", name, byteOffset, bitOffset, false));
    }

    protected void registerBoolOutputParameter(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        this.registerParameter(code, name, this.registerBoolOutputTag(code + "_TAG", name, byteOffset, bitOffset, writable));
    }

    protected void registerBoolVBitParameter(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        this.registerParameter(code, name, this.registerBoolVTag(code + "_TAG", name, byteOffset, bitOffset, writable));
    }

    protected void registerRealVParameter(String code, String name, int byteOffset, boolean writable) {
        this.registerParameter(code, name, this.registerRealVTag(code + "_TAG", name, byteOffset, writable));
    }

    protected void registerIntVParameter(String code, String name, int byteOffset, boolean writable) {
        this.registerParameter(code, name, this.registerIntVTag(code + "_TAG", name, byteOffset, writable));
    }
}

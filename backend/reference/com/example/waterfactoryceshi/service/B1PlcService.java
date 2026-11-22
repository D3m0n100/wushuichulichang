/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.apache.plc4x.java.api.PlcConnection
 *  org.apache.plc4x.java.api.messages.PlcReadRequest$Builder
 *  org.apache.plc4x.java.api.messages.PlcReadResponse
 *  org.apache.plc4x.java.api.messages.PlcWriteRequest
 *  org.apache.plc4x.java.api.messages.PlcWriteResponse
 *  org.apache.plc4x.java.api.types.PlcResponseCode
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Service
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.B1PlcConnectionPool;
import com.example.waterfactoryceshi.service.OverviewCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.apache.plc4x.java.api.messages.PlcWriteResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class B1PlcService {
    private static final Logger log = LoggerFactory.getLogger(B1PlcService.class);
    private final B1PlcConnectionPool connectionPool;
    private final OverviewCache overviewCache = new OverviewCache();
    @Value(value="${plc.b1.ip}")
    private String plcIp;
    @Value(value="${plc.b1.rack:0}")
    private int rack;
    @Value(value="${plc.b1.slot:1}")
    private int slot;
    private final Map<String, TagDef> tags = new LinkedHashMap<String, TagDef>();
    private final Map<String, DeviceDef> devices = new LinkedHashMap<String, DeviceDef>();
    private final Map<String, InstrumentDef> instruments = new LinkedHashMap<String, InstrumentDef>();
    private final Map<String, ParameterDef> parameters = new LinkedHashMap<String, ParameterDef>();

    public B1PlcService(B1PlcConnectionPool b1PlcConnectionPool) {
        this.connectionPool = b1PlcConnectionPool;
        this.initDefinitions();
    }

    @Scheduled(fixedDelay=3000L)
    public void backgroundFetch() {
        try {
            LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
            linkedHashMap.put("plc", this.buildPlcMeta());
            linkedHashMap.put("devices", this.readDevices());
            linkedHashMap.put("instruments", this.readInstruments());
            linkedHashMap.put("parameters", this.readParameters());
            this.overviewCache.set(linkedHashMap);
        }
        catch (Exception exception) {
            this.overviewCache.markFailure(exception.getMessage());
            log.warn("B1 PLC background fetch failed: {}", (Object)exception.getMessage());
        }
    }

    public Map<String, Object> readOverview() {
        OverviewCache.CacheResult cacheResult = this.overviewCache.getCached();
        if (cacheResult == null) {
            this.backgroundFetch();
            cacheResult = this.overviewCache.getCached();
        }
        return this.enrichOverview((Map)cacheResult.getValue(), cacheResult);
    }

    public Map<String, Object> readDevice(String string) {
        DeviceDef deviceDef = this.requireDevice(string);
        return this.buildDeviceState(deviceDef, this.readTags(deviceDef.getReadTags()));
    }

    public boolean writeManualMode(String string, boolean bl) {
        DeviceDef deviceDef = this.requireDevice(string);
        if (deviceDef.manualModeTagCode == null) {
            throw new IllegalArgumentException("Device does not support manual mode: " + string);
        }
        return this.writeBoolTag(deviceDef.manualModeTagCode, bl);
    }

    public boolean pulseCommand(String string, String string2, long l) {
        DeviceDef deviceDef = this.requireDevice(string);
        CommandWrite commandWrite = this.resolveCommandWrite(deviceDef, string2);
        if (commandWrite == null) {
            throw new IllegalArgumentException("Unsupported action for device " + string + ": " + string2);
        }
        return this.writeBoolTag(commandWrite.tagCode, commandWrite.value);
    }

    public boolean writeInstrumentConfig(String string, Float f, Float f2, Float f3) {
        boolean bl;
        InstrumentDef instrumentDef = this.instruments.get(string);
        if (instrumentDef == null) {
            throw new IllegalArgumentException("Unknown instrument: " + string);
        }
        boolean bl2 = true;
        if (f != null) {
            bl2 = bl2 && this.writeRealTag(instrumentDef.upperLimitTagCode, f.floatValue());
            bl = bl2;
        }
        if (f2 != null) {
            bl2 = bl2 && this.writeRealTag(instrumentDef.lowerLimitTagCode, f2.floatValue());
            bl = bl2;
        }
        if (f3 != null) {
            bl2 = bl2 && this.writeRealTag(instrumentDef.deviationTagCode, f3.floatValue());
        }
        return bl2;
    }

    public boolean writeParameter(String string, String string2) {
        ParameterDef parameterDef = this.parameters.get(string);
        if (parameterDef == null) {
            throw new IllegalArgumentException("Unknown B1 parameter: " + string);
        }
        TagDef tagDef = this.requireWritableTag(parameterDef.tagCode);
        switch (tagDef.type) {
            case BOOL: {
                return this.writeBoolTag(tagDef.code, Boolean.parseBoolean(string2));
            }
            case INT: {
                return this.writeIntTag(tagDef.code, Integer.parseInt(string2));
            }
            case REAL: {
                return this.writeRealTag(tagDef.code, Float.parseFloat(string2));
            }
        }
        throw new IllegalArgumentException("Unsupported B1 parameter type: " + (Object)((Object)tagDef.type));
    }

    private Map<String, Object> buildPlcMeta() {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        linkedHashMap.put("name", "B1");
        linkedHashMap.put("series", "S7-1200");
        linkedHashMap.put("ip", this.plcIp);
        linkedHashMap.put("rack", this.rack);
        linkedHashMap.put("slot", this.slot);
        return linkedHashMap;
    }

    private List<Map<String, Object>> readDevices() {
        ArrayList<Map<String, Object>> arrayList = new ArrayList<Map<String, Object>>();
        for (DeviceDef deviceDef : this.devices.values()) {
            arrayList.add(this.buildDeviceState(deviceDef, this.readTags(deviceDef.getReadTags())));
        }
        return arrayList;
    }

    private List<Map<String, Object>> readInstruments() {
        ArrayList<Map<String, Object>> arrayList = new ArrayList<Map<String, Object>>();
        for (InstrumentDef instrumentDef : this.instruments.values()) {
            Map<String, Object> map = this.readTags(instrumentDef.getReadTags());
            LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
            linkedHashMap.put("code", instrumentDef.code);
            linkedHashMap.put("name", instrumentDef.name);
            linkedHashMap.put("upperLimit", map.get(instrumentDef.upperLimitTagCode));
            linkedHashMap.put("lowerLimit", map.get(instrumentDef.lowerLimitTagCode));
            linkedHashMap.put("deviation", map.get(instrumentDef.deviationTagCode));
            linkedHashMap.put("output", map.get(instrumentDef.outputTagCode));
            arrayList.add(linkedHashMap);
        }
        return arrayList;
    }

    private List<Map<String, Object>> readParameters() {
        ArrayList<Map<String, Object>> arrayList = new ArrayList<Map<String, Object>>();
        for (ParameterDef parameterDef : this.parameters.values()) {
            TagDef tagDef = this.tags.get(parameterDef.tagCode);
            Map<String, Object> map = this.readTags(Arrays.asList(parameterDef.tagCode));
            LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
            linkedHashMap.put("code", parameterDef.code);
            linkedHashMap.put("name", parameterDef.name);
            linkedHashMap.put("address", tagDef.address);
            linkedHashMap.put("type", tagDef.type.name());
            linkedHashMap.put("writable", tagDef.writable);
            linkedHashMap.put("value", map.get(parameterDef.tagCode));
            arrayList.add(linkedHashMap);
        }
        return arrayList;
    }

    private Map<String, Object> buildDeviceState(DeviceDef deviceDef, Map<String, Object> map) {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        linkedHashMap.put("code", deviceDef.code);
        linkedHashMap.put("name", deviceDef.name);
        linkedHashMap.put("type", deviceDef.type);
        this.putIfTagExists(linkedHashMap, "remote", map, deviceDef.remoteTagCode);
        this.putIfTagExists(linkedHashMap, "running", map, deviceDef.runningTagCode);
        this.putIfTagExists(linkedHashMap, "fault", map, deviceDef.faultTagCode);
        this.putIfTagExists(linkedHashMap, "openPosition", map, deviceDef.openPositionTagCode);
        this.putIfTagExists(linkedHashMap, "closePosition", map, deviceDef.closePositionTagCode);
        this.putIfTagExists(linkedHashMap, "manualModeBit", map, deviceDef.manualModeTagCode);
        this.putIfTagExists(linkedHashMap, "autoModeBit", map, deviceDef.autoStartStopTagCode);
        return linkedHashMap;
    }

    private void putIfTagExists(Map<String, Object> map, String string, Map<String, Object> map2, String string2) {
        if (string2 != null) {
            map.put(string, map2.get(string2));
        }
    }

    private DeviceDef requireDevice(String string) {
        DeviceDef deviceDef = this.devices.get(string);
        if (deviceDef == null) {
            throw new IllegalArgumentException("Unknown B1 device: " + string);
        }
        return deviceDef;
    }

    private CommandWrite resolveCommandWrite(DeviceDef deviceDef, String string) {
        if (string == null || string.trim().isEmpty() || "PULSE".equalsIgnoreCase(string.trim())) {
            return deviceDef.startStopTagCode == null ? null : new CommandWrite(deviceDef.startStopTagCode, true);
        }
        String string2 = string.trim().toUpperCase();
        if ("START".equals(string2) || "STOP".equals(string2)) {
            return deviceDef.startStopTagCode == null ? null : new CommandWrite(deviceDef.startStopTagCode, "START".equals(string2));
        }
        if ("OPEN".equals(string2)) {
            return deviceDef.openCommandTagCode == null ? null : new CommandWrite(deviceDef.openCommandTagCode, true);
        }
        if ("CLOSE".equals(string2)) {
            return deviceDef.closeCommandTagCode == null ? null : new CommandWrite(deviceDef.closeCommandTagCode, true);
        }
        return null;
    }

    private Map<String, Object> readTags(Collection<String> collection) {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        PlcConnection plcConnection = null;
        boolean bl = false;
        try {
            plcConnection = this.connectionPool.borrowConnection();
            PlcReadRequest.Builder builder = plcConnection.readRequestBuilder();
            for (String object2 : collection) {
                TagDef tagDef = this.tags.get(object2);
                if (tagDef == null) continue;
                builder.addItem(tagDef.code, tagDef.address);
            }
            PlcReadResponse plcReadResponse = (PlcReadResponse)builder.build().execute().get(5L, TimeUnit.SECONDS);
            block15: for (String string : collection) {
                TagDef tagDef = this.tags.get(string);
                if (tagDef == null) continue;
                if (plcReadResponse.getResponseCode(tagDef.code) != PlcResponseCode.OK) {
                    linkedHashMap.put(tagDef.code, null);
                    continue;
                }
                switch (tagDef.type) {
                    case BOOL: {
                        linkedHashMap.put(tagDef.code, plcReadResponse.getBoolean(tagDef.code));
                        continue block15;
                    }
                    case INT: {
                        linkedHashMap.put(tagDef.code, plcReadResponse.getInteger(tagDef.code));
                        continue block15;
                    }
                    case REAL: {
                        linkedHashMap.put(tagDef.code, plcReadResponse.getFloat(tagDef.code));
                        continue block15;
                    }
                }
                linkedHashMap.put(tagDef.code, null);
            }
            LinkedHashMap<String, Object> linkedHashMap2 = linkedHashMap;
            return linkedHashMap2;
        }
        catch (Exception exception) {
            bl = true;
            throw new IllegalStateException("Failed to read B1 PLC tags: " + exception.getMessage(), exception);
        }
        finally {
            if (plcConnection != null) {
                try {
                    if (bl) {
                        this.connectionPool.invalidateConnection(plcConnection);
                    } else {
                        this.connectionPool.returnConnection(plcConnection);
                    }
                }
                catch (Exception exception) {}
            }
        }
    }

    private boolean writeBoolTag(String string, boolean bl) {
        TagDef tagDef = this.requireWritableTag(string, TagType.BOOL);
        PlcConnection plcConnection = null;
        try {
            boolean bl2;
            plcConnection = this.connectionPool.borrowConnection();
            PlcWriteRequest plcWriteRequest = plcConnection.writeRequestBuilder().addItem(tagDef.code, tagDef.address, new Object[]{bl}).build();
            PlcWriteResponse plcWriteResponse = (PlcWriteResponse)plcWriteRequest.execute().get(5L, TimeUnit.SECONDS);
            boolean bl3 = plcWriteResponse.getResponseCode(tagDef.code) == PlcResponseCode.OK;
            boolean bl4 = bl3;
            if (bl3) {
                this.overviewCache.invalidate();
            }
            boolean bl5 = bl2 = bl3;
            return bl5;
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to write BOOL tag " + string + ": " + exception.getMessage(), exception);
        }
        finally {
            if (plcConnection != null) {
                this.connectionPool.returnConnection(plcConnection);
            }
        }
    }

    private boolean writeIntTag(String string, int n) {
        TagDef tagDef = this.requireWritableTag(string, TagType.INT);
        PlcConnection plcConnection = null;
        try {
            boolean bl;
            plcConnection = this.connectionPool.borrowConnection();
            PlcWriteRequest plcWriteRequest = plcConnection.writeRequestBuilder().addItem(tagDef.code, tagDef.address, new Object[]{n}).build();
            PlcWriteResponse plcWriteResponse = (PlcWriteResponse)plcWriteRequest.execute().get(5L, TimeUnit.SECONDS);
            boolean bl2 = plcWriteResponse.getResponseCode(tagDef.code) == PlcResponseCode.OK;
            boolean bl3 = bl2;
            if (bl2) {
                this.overviewCache.invalidate();
            }
            boolean bl4 = bl = bl2;
            return bl4;
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to write INT tag " + string + ": " + exception.getMessage(), exception);
        }
        finally {
            if (plcConnection != null) {
                this.connectionPool.returnConnection(plcConnection);
            }
        }
    }

    private boolean writeRealTag(String string, float f) {
        TagDef tagDef = this.requireWritableTag(string, TagType.REAL);
        PlcConnection plcConnection = null;
        try {
            boolean bl;
            plcConnection = this.connectionPool.borrowConnection();
            PlcWriteRequest plcWriteRequest = plcConnection.writeRequestBuilder().addItem(tagDef.code, tagDef.address, new Object[]{Float.valueOf(f)}).build();
            PlcWriteResponse plcWriteResponse = (PlcWriteResponse)plcWriteRequest.execute().get(5L, TimeUnit.SECONDS);
            boolean bl2 = plcWriteResponse.getResponseCode(tagDef.code) == PlcResponseCode.OK;
            boolean bl3 = bl2;
            if (bl2) {
                this.overviewCache.invalidate();
            }
            boolean bl4 = bl = bl2;
            return bl4;
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to write REAL tag " + string + ": " + exception.getMessage(), exception);
        }
        finally {
            if (plcConnection != null) {
                this.connectionPool.returnConnection(plcConnection);
            }
        }
    }

    private boolean pulseBoolTag(String string, long l) {
        long l2 = l > 0L ? l : 300L;
        long l3 = l2;
        if (!this.writeBoolTag(string, true)) {
            return false;
        }
        try {
            Thread.sleep(l2);
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pulse command interrupted", interruptedException);
        }
        return this.writeBoolTag(string, false);
    }

    private TagDef requireWritableTag(String string) {
        TagDef tagDef = this.tags.get(string);
        if (tagDef == null) {
            throw new IllegalArgumentException("Unknown B1 tag: " + string);
        }
        if (!tagDef.writable) {
            throw new IllegalArgumentException("Tag is read-only: " + string);
        }
        return tagDef;
    }

    private TagDef requireWritableTag(String string, TagType tagType) {
        TagDef tagDef = this.requireWritableTag(string);
        if (tagDef.type != tagType) {
            throw new IllegalArgumentException("Tag type mismatch for " + string);
        }
        return tagDef;
    }

    private void initDefinitions() {
        this.registerValve("B1_QM1", "\u8c03\u8282\u6c60\u7535\u52a8\u9600\u95e81", 1, 0);
        this.registerValve("B1_QM2", "\u8c03\u8282\u6c60\u7535\u52a8\u9600\u95e82", 1, 2);
        this.registerValve("B1_QM3", "\u8c03\u8282\u6c60\u7535\u52a8\u9600\u95e83", 1, 4);
        this.registerValve("B2_QM1", "\u4e00\u671f\u7535\u52a8\u9600\u95e81", 1, 6);
        this.registerValve("B2_QM2", "\u4e00\u671f\u7535\u52a8\u9600\u95e82", 1, 8);
        this.registerValve("B2_QM3", "\u4e00\u671f\u7535\u52a8\u9600\u95e83", 1, 10);
        this.registerValve("B2_QM4", "\u4e00\u671f\u7535\u52a8\u9600\u95e84", 1, 12);
        this.registerValve("B2_QM5", "\u4e00\u671f\u7535\u52a8\u9600\u95e85", 1, 14);
        this.registerMotor("B1_HW1", "\u7c97\u683c\u6805\u6f5c\u6c34\u6405\u62cc\u673a1", "mixer", 1, 16, null);
        this.registerMotor("B1_HW2", "\u7c97\u683c\u6805\u6f5c\u6c34\u6405\u62cc\u673a2", "mixer", 1, 18, this.boolTag("B1_HW2_AUTO", "\u7c97\u683c\u6805\u6f5c\u6c34\u6405\u62cc\u673a2\u81ea\u52a8\u542f\u505c", 1, 18, 5, false));
        this.registerMotor("B1_GP1", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf51", "pump", 1, 20, null);
        this.registerMotor("B1_GP2", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf52", "pump", this.boolTag("B1_GP2_REMOTE", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf52\u8fdc\u7a0b", 1, 22, 0, false), this.boolTag("B1_GP2_RUNNING", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf52\u8fd0\u884c", 1, 22, 1, false), this.boolTag("B1_GP2_FAULT", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf52\u6545\u969c", 1, 22, 2, false), this.boolTag("B1_GP2_MANUAL", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf52\u624b\u81ea\u52a8", 1, 22, 3, true), this.boolTag("B1_GP2_START_STOP", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf52\u624b\u52a8\u542f\u505c", 1, 22, 4, true), null);
        this.registerMotor("B1_GP3", "\u8c03\u8282\u6c60\u63d0\u5347\u6cf53", "pump", 1, 24, null);
        this.registerMotor("T3_HN1", "\u6c61\u6ce5\u6d53\u7f29\u6c60\u522e\u6ce5\u673a", "scraper", 1, 26, null);
        this.registerInstrument("T3_RIT01", "\u6c61\u6ce5\u754c\u9762\u4eea", 2, 4, 8, 12, 16);
        this.registerInstrument("B1_LIT01", "\u7c97\u683c\u6805\u8d85\u58f0\u6ce2\u6db2\u4f4d\u8ba1", 2, 24, 28, 32, 36);
        this.registerInstrument("B1_FIT01", "\u4e00\u671fCASS\u6c60\u8fdb\u6c34\u6d41\u91cf\u8ba1", 2, 44, 48, 52, 56);
        this.registerParameter("B1_GAS_CO", "\u4e00\u6c27\u5316\u78b3", this.realTag("B1_GAS_CO_TAG", "\u4e00\u6c27\u5316\u78b3", 2, 60, false));
        this.registerParameter("B1_GAS_O2", "\u6c27\u6c14", this.realTag("B1_GAS_O2_TAG", "\u6c27\u6c14", 2, 64, false));
        this.registerParameter("B1_GAS_H2S", "\u786b\u5316\u6c22", this.realTag("B1_GAS_H2S_TAG", "\u786b\u5316\u6c22", 2, 68, false));
        this.registerParameter("B1_GAS_CH4", "\u7532\u70f7", this.realTag("B1_GAS_CH4_TAG", "\u7532\u70f7", 2, 72, false));
        this.registerParameter("B1_INFLOW_COD", "COD", this.realTag("B1_INFLOW_COD_TAG", "COD", 2, 76, false));
        this.registerParameter("B1_INFLOW_NH3N", "\u6c28\u6c2e", this.realTag("B1_INFLOW_NH3N_TAG", "\u6c28\u6c2e", 2, 80, false));
        this.registerParameter("B1_INFLOW_TP", "\u603b\u78f7", this.realTag("B1_INFLOW_TP_TAG", "\u603b\u78f7", 2, 84, false));
        this.registerParameter("B1_INFLOW_TN", "\u603b\u6c2e", this.realTag("B1_INFLOW_TN_TAG", "\u603b\u6c2e", 2, 88, false));
        this.registerParameter("B1_INFLOW_PH", "PH", this.realTag("B1_INFLOW_PH_TAG", "PH", 2, 92, false));
        this.registerParameter("B1_INFLOW_TURBIDITY", "\u6d4a\u5ea6", this.realTag("B1_INFLOW_TURBIDITY_TAG", "\u6d4a\u5ea6", 2, 96, false));
        this.registerParameter("B1_PHASE1_FLOWMETER", "\u4e00\u671f\u6d41\u91cf\u8ba1", this.realTag("B1_PHASE1_FLOWMETER_TAG", "\u4e00\u671f\u6d41\u91cf\u8ba1", 2, 100, false));
        this.registerParameter("B1_PHASE2_FLOWMETER", "\u4e8c\u671f\u6d41\u91cf\u8ba1", this.realTag("B1_PHASE2_FLOWMETER_TAG", "\u4e8c\u671f\u6d41\u91cf\u8ba1", 2, 104, false));
        this.registerParameter("B1_PHASE1_FLOWMETER_TOTALIZER", "\u4e00\u671f\u6d41\u91cf\u8ba1\u7d2f\u8ba1\u6d41\u91cf", this.realTag("B1_PHASE1_FLOWMETER_TOTALIZER_TAG", "\u4e00\u671f\u6d41\u91cf\u8ba1\u7d2f\u8ba1\u6d41\u91cf", 2, 108, false));
        this.registerParameter("B1_PHASE2_FLOWMETER_TOTALIZER", "\u4e8c\u671f\u6d41\u91cf\u8ba1\u7d2f\u8ba1\u6d41\u91cf", this.realTag("B1_PHASE2_FLOWMETER_TOTALIZER_TAG", "\u4e8c\u671f\u6d41\u91cf\u8ba1\u7d2f\u8ba1\u6d41\u91cf", 2, 112, false));
        this.registerParameter("B1_HW_RUNTIME_HOURS", "B1-HW1~2\u5df2\u8fd0\u884c\u65f6\u95f4\u65f6", this.intTag("B1_HW_RUNTIME_HOURS_TAG", "B1-HW1~2\u5df2\u8fd0\u884c\u65f6\u95f4\u65f6", 3, 0, false));
        this.registerParameter("B1_HW_RUNTIME_MINUTES", "B1-HW1~2\u5df2\u8fd0\u884c\u65f6\u95f4\u5206", this.intTag("B1_HW_RUNTIME_MINUTES_TAG", "B1-HW1~2\u5df2\u8fd0\u884c\u65f6\u95f4\u5206", 3, 2, false));
        this.registerParameter("B1_HW_RUNTIME_SECONDS", "B1-HW1~2\u5df2\u8fd0\u884c\u65f6\u95f4\u79d2", this.intTag("B1_HW_RUNTIME_SECONDS_TAG", "B1-HW1~2\u5df2\u8fd0\u884c\u65f6\u95f4\u79d2", 3, 4, false));
        this.registerParameter("B1_HW_STOPPED_HOURS", "B1-HW1~2\u5df2\u505c\u6b62\u65f6\u95f4\u65f6", this.intTag("B1_HW_STOPPED_HOURS_TAG", "B1-HW1~2\u5df2\u505c\u6b62\u65f6\u95f4\u65f6", 3, 6, false));
        this.registerParameter("B1_HW_STOPPED_MINUTES", "B1-HW1~2\u5df2\u505c\u6b62\u65f6\u95f4\u5206", this.intTag("B1_HW_STOPPED_MINUTES_TAG", "B1-HW1~2\u5df2\u505c\u6b62\u65f6\u95f4\u5206", 3, 8, false));
        this.registerParameter("B1_HW_STOPPED_SECONDS", "B1-HW1~2\u5df2\u505c\u6b62\u65f6\u95f4\u79d2", this.intTag("B1_HW_STOPPED_SECONDS_TAG", "B1-HW1~2\u5df2\u505c\u6b62\u65f6\u95f4\u79d2", 3, 10, false));
        this.registerParameter("B1_HW_RUN_SET_HOURS", "B1-HW1~2\u8fd0\u884c\u8ba1\u65f6\u65f6", this.intTag("B1_HW_RUN_SET_HOURS_TAG", "B1-HW1~2\u8fd0\u884c\u8ba1\u65f6\u65f6", 3, 12, true));
        this.registerParameter("B1_HW_RUN_SET_MINUTES", "B1-HW1~2\u8fd0\u884c\u8ba1\u65f6\u5206", this.intTag("B1_HW_RUN_SET_MINUTES_TAG", "B1-HW1~2\u8fd0\u884c\u8ba1\u65f6\u5206", 3, 14, true));
        this.registerParameter("B1_HW_RUN_SET_SECONDS", "B1-HW1~2\u8fd0\u884c\u8ba1\u65f6\u79d2", this.intTag("B1_HW_RUN_SET_SECONDS_TAG", "B1-HW1~2\u8fd0\u884c\u8ba1\u65f6\u79d2", 3, 16, true));
        this.registerParameter("B1_HW_STOP_SET_HOURS", "B1-HW1~2\u505c\u6b62\u8ba1\u65f6\u65f6", this.intTag("B1_HW_STOP_SET_HOURS_TAG", "B1-HW1~2\u505c\u6b62\u8ba1\u65f6\u65f6", 3, 18, true));
        this.registerParameter("B1_HW_STOP_SET_MINUTES", "B1-HW1~2\u505c\u6b62\u8ba1\u65f6\u5206", this.intTag("B1_HW_STOP_SET_MINUTES_TAG", "B1-HW1~2\u505c\u6b62\u8ba1\u65f6\u5206", 3, 20, true));
        this.registerParameter("B1_HW_STOP_SET_SECONDS", "B1-HW1~2\u505c\u6b62\u8ba1\u65f6\u79d2", this.intTag("B1_HW_STOP_SET_SECONDS_TAG", "B1-HW1~2\u505c\u6b62\u8ba1\u65f6\u79d2", 3, 22, true));
        this.registerParameter("B1_GP_SINGLE_LEVEL", "B1-GP1~3\u5355\u53f0\u6db2\u4f4d", this.realTag("B1_GP_SINGLE_LEVEL_TAG", "B1-GP1~3\u5355\u53f0\u6db2\u4f4d", 3, 24, true));
        this.registerParameter("B1_GP_DOUBLE_LEVEL", "B1-GP1~3\u4e24\u53f0\u6db2\u4f4d", this.realTag("B1_GP_DOUBLE_LEVEL_TAG", "B1-GP1~3\u4e24\u53f0\u6db2\u4f4d", 3, 28, true));
        this.registerParameter("B1_GP_STOP_LEVEL", "B1-GP1~3\u505c\u6b62\u6db2\u4f4d", this.realTag("B1_GP_STOP_LEVEL_TAG", "B1-GP1~3\u505c\u6b62\u6db2\u4f4d", 3, 32, true));
    }

    private void registerValve(String string, String string2, int n, int n2) {
        String string3 = this.boolTag(string + "_REMOTE", string2 + "\u8fdc\u7a0b", n, n2, 0, false);
        String string4 = this.boolTag(string + "_OPEN_POS", string2 + "\u5f00\u5230\u4f4d", n, n2, 1, false);
        String string5 = this.boolTag(string + "_CLOSE_POS", string2 + "\u5173\u5230\u4f4d", n, n2, 2, false);
        String string6 = this.boolTag(string + "_FAULT", string2 + "\u6545\u969c", n, n2, 3, false);
        String string7 = this.boolTag(string + "_MANUAL_MODE", string2 + "\u624b\u81ea\u52a8", n, n2, 4, true);
        String string8 = this.boolTag(string + "_OPEN_CMD", string2 + "\u624b\u52a8\u5f00\u9600", n, n2, 5, true);
        String string9 = this.boolTag(string + "_CLOSE_CMD", string2 + "\u624b\u52a8\u5173\u9600", n, n2, 6, true);
        this.devices.put(string, new DeviceDef(string, string2, "valve", string3, null, string6, string4, string5, string7, string8, string9, null, null));
        this.registerParameter(string + "_REMOTE", string2 + "\u8fdc\u7a0b", string3);
        this.registerParameter(string + "_OPEN_POS", string2 + "\u5f00\u5230\u4f4d", string4);
        this.registerParameter(string + "_CLOSE_POS", string2 + "\u5173\u5230\u4f4d", string5);
        this.registerParameter(string + "_FAULT", string2 + "\u6545\u969c", string6);
        this.registerParameter(string + "_MANUAL_MODE", string2 + "\u624b\u81ea\u52a8", string7);
        this.registerParameter(string + "_OPEN_CMD", string2 + "\u624b\u52a8\u5f00\u9600", string8);
        this.registerParameter(string + "_CLOSE_CMD", string2 + "\u624b\u52a8\u5173\u9600", string9);
    }

    private void registerMotor(String string, String string2, String string3, int n, int n2, String string4) {
        String string5 = this.boolTag(string + "_REMOTE", string2 + "\u8fdc\u7a0b", n, n2, 0, false);
        String string6 = this.boolTag(string + "_RUNNING", string2 + "\u8fd0\u884c", n, n2, 1, false);
        String string7 = this.boolTag(string + "_FAULT", string2 + "\u6545\u969c", n, n2, 2, false);
        String string8 = this.boolTag(string + "_MANUAL_MODE", string2 + "\u624b\u81ea\u52a8", n, n2, 3, true);
        String string9 = this.boolTag(string + "_START_STOP", string2 + "\u624b\u52a8\u542f\u505c", n, n2, 4, true);
        this.registerMotor(string, string2, string3, string5, string6, string7, string8, string9, string4);
    }

    private void registerMotor(String string, String string2, String string3, String string4, String string5, String string6, String string7, String string8, String string9) {
        this.devices.put(string, new DeviceDef(string, string2, string3, string4, string5, string6, null, null, string7, null, null, string8, string9));
        this.registerParameter(string + "_REMOTE", string2 + "\u8fdc\u7a0b", string4);
        this.registerParameter(string + "_RUNNING", string2 + "\u8fd0\u884c", string5);
        this.registerParameter(string + "_FAULT", string2 + "\u6545\u969c", string6);
        this.registerParameter(string + "_MANUAL_MODE", string2 + "\u624b\u81ea\u52a8", string7);
        this.registerParameter(string + "_START_STOP", string2 + "\u624b\u52a8\u542f\u505c", string8);
        if (string9 != null) {
            this.registerParameter(string + "_AUTO", string2 + "\u81ea\u52a8\u542f\u505c", string9);
        }
    }

    private void registerInstrument(String string, String string2, int n, int n2, int n3, int n4, int n5) {
        String string3 = this.realTag(string + "_UPPER", string2 + "\u4eea\u8868\u4e0a\u9650\u503c", n, n2, true);
        String string4 = this.realTag(string + "_LOWER", string2 + "\u4eea\u8868\u4e0b\u9650\u503c", n, n3, true);
        String string5 = this.realTag(string + "_DEVIATION", string2 + "\u504f\u5dee\u503c", n, n4, true);
        String string6 = this.realTag(string + "_OUTPUT", string2 + "\u8f93\u51fa\u503c", n, n5, false);
        this.instruments.put(string, new InstrumentDef(string, string2, string3, string4, string5, string6));
    }

    private String boolTag(String string, String string2, int n, int n2, int n3, boolean bl) {
        return this.registerTag(string, string2, String.format("%%DB%d.DBX%d.%d:BOOL", n, n2, n3), TagType.BOOL, bl);
    }

    private String intTag(String string, String string2, int n, int n2, boolean bl) {
        return this.registerTag(string, string2, String.format("%%DB%d.DBW%d:INT", n, n2), TagType.INT, bl);
    }

    private String realTag(String string, String string2, int n, int n2, boolean bl) {
        return this.registerTag(string, string2, String.format("%%DB%d.DBD%d:REAL", n, n2), TagType.REAL, bl);
    }

    private String registerTag(String string, String string2, String string3, TagType tagType, boolean bl) {
        this.tags.put(string, new TagDef(string, string2, string3, tagType, bl));
        return string;
    }

    private void registerParameter(String string, String string2, String string3) {
        this.parameters.put(string, new ParameterDef(string, string2, string3));
    }

    private static void addIfPresent(List<String> list, String string) {
        if (string != null) {
            list.add(string);
        }
    }

    private Map<String, Object> enrichOverview(Map<String, Object> map, OverviewCache.CacheResult cacheResult) {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(map);
        LinkedHashMap<String, String> linkedHashMap2 = (LinkedHashMap<String, String>)linkedHashMap.get("plc");
        if (linkedHashMap2 == null) {
            linkedHashMap2 = new LinkedHashMap<String, String>();
            linkedHashMap.put("plc", linkedHashMap2);
        }
        linkedHashMap2.put("state", cacheResult.isStale() ? "STALE" : "ONLINE");
        linkedHashMap2.put("stale", (String)((Object)Boolean.valueOf(cacheResult.isStale())));
        linkedHashMap2.put("cacheHit", (String)((Object)Boolean.valueOf(cacheResult.isCacheHit())));
        linkedHashMap2.put("cacheAgeMs", (String)((Object)Long.valueOf(cacheResult.getCacheAgeMs())));
        linkedHashMap2.put("lastSuccessAt", (String)((Object)Long.valueOf(cacheResult.getLastSuccessAt())));
        if (cacheResult.getLastFailureAt() > 0L) {
            linkedHashMap2.put("lastFailureAt", (String)((Object)Long.valueOf(cacheResult.getLastFailureAt())));
            linkedHashMap2.put("lastFailureMessage", cacheResult.getLastFailureMessage());
        }
        return linkedHashMap;
    }

    private static class DeviceDef {
        private final String code;
        private final String name;
        private final String type;
        private final String remoteTagCode;
        private final String runningTagCode;
        private final String faultTagCode;
        private final String openPositionTagCode;
        private final String closePositionTagCode;
        private final String manualModeTagCode;
        private final String openCommandTagCode;
        private final String closeCommandTagCode;
        private final String startStopTagCode;
        private final String autoStartStopTagCode;

        private DeviceDef(String string, String string2, String string3, String string4, String string5, String string6, String string7, String string8, String string9, String string10, String string11, String string12, String string13) {
            this.code = string;
            this.name = string2;
            this.type = string3;
            this.remoteTagCode = string4;
            this.runningTagCode = string5;
            this.faultTagCode = string6;
            this.openPositionTagCode = string7;
            this.closePositionTagCode = string8;
            this.manualModeTagCode = string9;
            this.openCommandTagCode = string10;
            this.closeCommandTagCode = string11;
            this.startStopTagCode = string12;
            this.autoStartStopTagCode = string13;
        }

        private List<String> getReadTags() {
            ArrayList<String> arrayList = new ArrayList<String>();
            B1PlcService.addIfPresent(arrayList, this.remoteTagCode);
            B1PlcService.addIfPresent(arrayList, this.runningTagCode);
            B1PlcService.addIfPresent(arrayList, this.faultTagCode);
            B1PlcService.addIfPresent(arrayList, this.openPositionTagCode);
            B1PlcService.addIfPresent(arrayList, this.closePositionTagCode);
            B1PlcService.addIfPresent(arrayList, this.manualModeTagCode);
            B1PlcService.addIfPresent(arrayList, this.autoStartStopTagCode);
            return arrayList;
        }
    }

    private static class CommandWrite {
        private final String tagCode;
        private final boolean value;

        private CommandWrite(String string, boolean bl) {
            this.tagCode = string;
            this.value = bl;
        }
    }

    private static class InstrumentDef {
        private final String code;
        private final String name;
        private final String upperLimitTagCode;
        private final String lowerLimitTagCode;
        private final String deviationTagCode;
        private final String outputTagCode;

        private InstrumentDef(String string, String string2, String string3, String string4, String string5, String string6) {
            this.code = string;
            this.name = string2;
            this.upperLimitTagCode = string3;
            this.lowerLimitTagCode = string4;
            this.deviationTagCode = string5;
            this.outputTagCode = string6;
        }

        private List<String> getReadTags() {
            return Arrays.asList(this.upperLimitTagCode, this.lowerLimitTagCode, this.deviationTagCode, this.outputTagCode);
        }
    }

    private static class ParameterDef {
        private final String code;
        private final String name;
        private final String tagCode;

        private ParameterDef(String string, String string2, String string3) {
            this.code = string;
            this.name = string2;
            this.tagCode = string3;
        }
    }

    private static class TagDef {
        private final String code;
        private final String name;
        private final String address;
        private final TagType type;
        private final boolean writable;

        private TagDef(String string, String string2, String string3, TagType tagType, boolean bl) {
            this.code = string;
            this.name = string2;
            this.address = string3;
            this.type = tagType;
            this.writable = bl;
        }
    }

    private static enum TagType {
        BOOL,
        INT,
        REAL;

    }
}

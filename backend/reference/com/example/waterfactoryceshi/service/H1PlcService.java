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

import com.example.waterfactoryceshi.service.H1PlcConnectionPool;
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
public class H1PlcService {
    private static final Logger log = LoggerFactory.getLogger(H1PlcService.class);
    private final H1PlcConnectionPool connectionPool;
    private final OverviewCache overviewCache = new OverviewCache();
    @Value(value="${plc.h1.ip}")
    private String plcIp;
    @Value(value="${plc.h1.rack:0}")
    private int rack;
    @Value(value="${plc.h1.slot:1}")
    private int slot;
    private final Map<String, TagDef> tags = new LinkedHashMap<String, TagDef>();
    private final Map<String, DeviceDef> devices = new LinkedHashMap<String, DeviceDef>();
    private final Map<String, InstrumentDef> instruments = new LinkedHashMap<String, InstrumentDef>();
    private final Map<String, TimerDef> timers = new LinkedHashMap<String, TimerDef>();
    private final Map<String, LogicConfigDef> logicConfigs = new LinkedHashMap<String, LogicConfigDef>();
    private final Map<String, ScalarDef> communicationValues = new LinkedHashMap<String, ScalarDef>();

    public H1PlcService(H1PlcConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.initDefinitions();
    }

    @Scheduled(fixedDelay=3000L)
    public void backgroundFetch() {
        try {
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("plc", this.buildPlcMeta());
            result.put("devices", this.readDevices());
            result.put("instruments", this.readInstruments());
            result.put("timers", this.readTimers());
            result.put("logicConfigs", this.readLogicConfigs());
            result.put("communicationValues", this.readCommunicationValues());
            result.put("parameters", this.readParameters());
            result.put("catalogDevices", this.catalogDevices());
            this.overviewCache.set(result);
        }
        catch (Exception ex) {
            this.overviewCache.markFailure(ex.getMessage());
            log.warn("H1 PLC background fetch failed: {}", (Object)ex.getMessage());
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

    public Map<String, Object> catalog() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("plc", this.buildPlcMeta());
        result.put("devices", this.catalogDevices());
        result.put("parameters", this.catalogParameters());
        return result;
    }

    public Map<String, Object> readDevice(String deviceCode) {
        DeviceDef device = this.requireDevice(deviceCode);
        return this.buildDeviceState(device, this.readTags(device.getReadTags()));
    }

    public boolean writeManualMode(String deviceCode, boolean enabled) {
        DeviceDef device = this.requireDevice(deviceCode);
        return this.writeBoolTag(device.manualModeTagCode, enabled);
    }

    public boolean pulseCommand(String deviceCode, String action, long pulseMs) {
        DeviceDef device = this.requireDevice(deviceCode);
        CommandWrite commandWrite = this.resolveCommandWrite(device, action);
        if (commandWrite == null) {
            throw new IllegalArgumentException("Unsupported action for device " + deviceCode + ": " + action);
        }
        return this.writeBoolTag(commandWrite.tagCode, commandWrite.value);
    }

    public boolean writeFrequencySetpoint(String deviceCode, float value) {
        DeviceDef device = this.requireDevice(deviceCode);
        if (device.frequencySetpointTagCode == null) {
            throw new IllegalArgumentException("Device does not support frequency setpoint: " + deviceCode);
        }
        return this.writeRealTag(device.frequencySetpointTagCode, value);
    }

    public boolean writeInstrumentConfig(String instrumentCode, Float upperLimit, Float lowerLimit, Float deviation) {
        InstrumentDef instrument = this.instruments.get(instrumentCode);
        if (instrument == null) {
            throw new IllegalArgumentException("Unknown instrument: " + instrumentCode);
        }
        boolean success = true;
        if (upperLimit != null) {
            boolean bl = success = success && this.writeRealTag(instrument.upperLimitTagCode, upperLimit.floatValue());
        }
        if (lowerLimit != null) {
            boolean bl = success = success && this.writeRealTag(instrument.lowerLimitTagCode, lowerLimit.floatValue());
        }
        if (deviation != null) {
            success = success && this.writeRealTag(instrument.deviationTagCode, deviation.floatValue());
        }
        return success;
    }

    public boolean writeTimerConfig(String timerCode, Integer runHours, Integer runMinutes, Integer runSeconds, Integer stopHours, Integer stopMinutes, Integer stopSeconds) {
        TimerDef timer = this.timers.get(timerCode);
        if (timer == null) {
            throw new IllegalArgumentException("Unknown timer group: " + timerCode);
        }
        boolean success = true;
        if (runHours != null) {
            boolean bl = success = success && this.writeIntTag(timer.runHoursSetTagCode, runHours);
        }
        if (runMinutes != null) {
            boolean bl = success = success && this.writeIntTag(timer.runMinutesSetTagCode, runMinutes);
        }
        if (runSeconds != null) {
            boolean bl = success = success && this.writeIntTag(timer.runSecondsSetTagCode, runSeconds);
        }
        if (stopHours != null) {
            boolean bl = success = success && this.writeIntTag(timer.stopHoursSetTagCode, stopHours);
        }
        if (stopMinutes != null) {
            boolean bl = success = success && this.writeIntTag(timer.stopMinutesSetTagCode, stopMinutes);
        }
        if (stopSeconds != null) {
            success = success && this.writeIntTag(timer.stopSecondsSetTagCode, stopSeconds);
        }
        return success;
    }

    public boolean writeLogicConfig(String configCode, Float startLevel, Float stopLevel) {
        LogicConfigDef config = this.logicConfigs.get(configCode);
        if (config == null) {
            throw new IllegalArgumentException("Unknown logic config: " + configCode);
        }
        boolean success = true;
        if (startLevel != null && config.startLevelTagCode != null) {
            boolean bl = success = success && this.writeRealTag(config.startLevelTagCode, startLevel.floatValue());
        }
        if (stopLevel != null && config.stopLevelTagCode != null) {
            success = success && this.writeRealTag(config.stopLevelTagCode, stopLevel.floatValue());
        }
        return success;
    }

    public boolean writeParameter(String parameterCode, String value) {
        TagDef tag = this.tags.get(parameterCode);
        if (tag == null) {
            throw new IllegalArgumentException("Unknown H1 parameter: " + parameterCode);
        }
        if (!tag.writable) {
            throw new IllegalArgumentException("Parameter is read-only: " + parameterCode);
        }
        switch (tag.type) {
            case BOOL: {
                return this.writeBoolTag(parameterCode, this.parseBooleanValue(value));
            }
            case REAL: {
                return this.writeRealTag(parameterCode, Float.parseFloat(value));
            }
            case INT: {
                return this.writeIntTag(parameterCode, Integer.parseInt(value));
            }
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + (Object)((Object)tag.type));
    }

    private Map<String, Object> buildPlcMeta() {
        LinkedHashMap<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("name", "H1");
        meta.put("series", "S7-1500");
        meta.put("ip", this.plcIp);
        meta.put("rack", this.rack);
        meta.put("slot", this.slot);
        return meta;
    }

    private List<Map<String, Object>> readDevices() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DeviceDef device : this.devices.values()) {
            result.add(this.buildDeviceState(device, this.readTags(device.getReadTags())));
        }
        return result;
    }

    private List<Map<String, Object>> catalogDevices() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DeviceDef device : this.devices.values()) {
            LinkedHashMap<String, String> item = new LinkedHashMap<String, String>();
            item.put("code", device.code);
            item.put("name", device.name);
            item.put("type", device.type);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> readInstruments() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (InstrumentDef instrument : this.instruments.values()) {
            Map<String, Object> values = this.readTags(instrument.getReadTags());
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", instrument.code);
            item.put("name", instrument.name);
            item.put("upperLimit", values.get(instrument.upperLimitTagCode));
            item.put("lowerLimit", values.get(instrument.lowerLimitTagCode));
            item.put("deviation", values.get(instrument.deviationTagCode));
            item.put("output", values.get(instrument.outputTagCode));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> readTimers() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (TimerDef timer : this.timers.values()) {
            Map<String, Object> values = this.readTags(timer.getReadTags());
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", timer.code);
            item.put("name", timer.name);
            item.put("runElapsedHours", values.get(timer.runHoursElapsedTagCode));
            item.put("runElapsedMinutes", values.get(timer.runMinutesElapsedTagCode));
            item.put("runElapsedSeconds", values.get(timer.runSecondsElapsedTagCode));
            item.put("stopElapsedHours", values.get(timer.stopHoursElapsedTagCode));
            item.put("stopElapsedMinutes", values.get(timer.stopMinutesElapsedTagCode));
            item.put("stopElapsedSeconds", values.get(timer.stopSecondsElapsedTagCode));
            item.put("runSetHours", values.get(timer.runHoursSetTagCode));
            item.put("runSetMinutes", values.get(timer.runMinutesSetTagCode));
            item.put("runSetSeconds", values.get(timer.runSecondsSetTagCode));
            item.put("stopSetHours", values.get(timer.stopHoursSetTagCode));
            item.put("stopSetMinutes", values.get(timer.stopMinutesSetTagCode));
            item.put("stopSetSeconds", values.get(timer.stopSecondsSetTagCode));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> readLogicConfigs() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (LogicConfigDef config : this.logicConfigs.values()) {
            Map<String, Object> values = this.readTags(config.getReadTags());
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", config.code);
            item.put("name", config.name);
            this.putIfTagExists(item, "startLevel", values, config.startLevelTagCode);
            this.putIfTagExists(item, "stopLevel", values, config.stopLevelTagCode);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> readCommunicationValues() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ScalarDef scalar : this.communicationValues.values()) {
            Map<String, Object> values = this.readTags(Arrays.asList(scalar.tagCode));
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", scalar.code);
            item.put("name", scalar.name);
            item.put("value", values.get(scalar.tagCode));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> catalogParameters() {
        LinkedHashMap<String, Map<String, Object>> ordered = new LinkedHashMap<String, Map<String, Object>>();
        this.appendInstrumentParameters(ordered);
        this.appendTimerParameters(ordered);
        this.appendLogicParameters(ordered);
        this.appendCommunicationParameters(ordered);
        return new ArrayList<Map<String, Object>>(ordered.values());
    }

    private List<Map<String, Object>> readParameters() {
        List<Map<String, Object>> catalog = this.catalogParameters();
        ArrayList<String> tagCodes = new ArrayList<String>();
        for (Map<String, Object> item : catalog) {
            tagCodes.add(String.valueOf(item.get("code")));
        }
        Map<String, Object> values = this.readTags(tagCodes);
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : catalog) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>(item);
            copy.put("value", values.get(String.valueOf(item.get("code"))));
            result.add(copy);
        }
        return result;
    }

    private Map<String, Object> buildDeviceState(DeviceDef device, Map<String, Object> values) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("code", device.code);
        item.put("name", device.name);
        item.put("type", device.type);
        this.putIfTagExists(item, "remote", values, device.remoteTagCode);
        this.putIfTagExists(item, "running", values, device.runningTagCode);
        this.putIfTagExists(item, "fault", values, device.faultTagCode);
        this.putIfTagExists(item, "openPosition", values, device.openPositionTagCode);
        this.putIfTagExists(item, "closePosition", values, device.closePositionTagCode);
        this.putIfTagExists(item, "manualModeBit", values, device.manualModeTagCode);
        this.putIfTagExists(item, "frequencyFeedback", values, device.frequencyFeedbackTagCode);
        this.putIfTagExists(item, "frequencySetpoint", values, device.frequencySetpointTagCode);
        return item;
    }

    private void putIfTagExists(Map<String, Object> target, String key, Map<String, Object> values, String tagCode) {
        if (tagCode != null) {
            target.put(key, values.get(tagCode));
        }
    }

    private void appendInstrumentParameters(Map<String, Map<String, Object>> ordered) {
        for (InstrumentDef instrument : this.instruments.values()) {
            this.addParameterTag(ordered, instrument.upperLimitTagCode, "instrument", instrument.code, instrument.name);
            this.addParameterTag(ordered, instrument.lowerLimitTagCode, "instrument", instrument.code, instrument.name);
            this.addParameterTag(ordered, instrument.deviationTagCode, "instrument", instrument.code, instrument.name);
            this.addParameterTag(ordered, instrument.outputTagCode, "instrument", instrument.code, instrument.name);
        }
    }

    private void appendTimerParameters(Map<String, Map<String, Object>> ordered) {
        for (TimerDef timer : this.timers.values()) {
            for (String tagCode : timer.getReadTags()) {
                this.addParameterTag(ordered, tagCode, "timer", timer.code, timer.name);
            }
        }
    }

    private void appendLogicParameters(Map<String, Map<String, Object>> ordered) {
        for (LogicConfigDef config : this.logicConfigs.values()) {
            for (String tagCode : config.getReadTags()) {
                this.addParameterTag(ordered, tagCode, "logic", config.code, config.name);
            }
        }
    }

    private void appendCommunicationParameters(Map<String, Map<String, Object>> ordered) {
        for (ScalarDef scalar : this.communicationValues.values()) {
            this.addParameterTag(ordered, scalar.tagCode, "communication", scalar.code, scalar.name);
        }
    }

    private void addParameterTag(Map<String, Map<String, Object>> ordered, String tagCode, String category, String ownerCode, String ownerName) {
        if (tagCode == null || ordered.containsKey(tagCode)) {
            return;
        }
        TagDef tag = this.tags.get(tagCode);
        if (tag == null) {
            return;
        }
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("code", tag.code);
        item.put("name", tag.name);
        item.put("address", tag.address);
        item.put("type", tag.type.name());
        item.put("writable", tag.writable);
        item.put("category", category);
        item.put("ownerCode", ownerCode);
        item.put("ownerName", ownerName);
        ordered.put(tagCode, item);
    }

    private boolean parseBooleanValue(String value) {
        if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid BOOL value: " + value);
    }

    private DeviceDef requireDevice(String deviceCode) {
        DeviceDef device = this.devices.get(deviceCode);
        if (device == null) {
            throw new IllegalArgumentException("Unknown H1 device: " + deviceCode);
        }
        return device;
    }

    private CommandWrite resolveCommandWrite(DeviceDef device, String action) {
        if (action == null) {
            return null;
        }
        String normalized = action.trim().toUpperCase();
        if ("START".equals(normalized) || "STOP".equals(normalized)) {
            return device.startStopTagCode == null ? null : new CommandWrite(device.startStopTagCode, "START".equals(normalized));
        }
        if ("OPEN".equals(normalized)) {
            return device.openCommandTagCode == null ? null : new CommandWrite(device.openCommandTagCode, true);
        }
        if ("CLOSE".equals(normalized)) {
            return device.closeCommandTagCode == null ? null : new CommandWrite(device.closeCommandTagCode, true);
        }
        return null;
    }

    private Map<String, Object> readTags(Collection<String> tagCodes) {
        LinkedHashMap<String, Comparable<Boolean>> values = new LinkedHashMap<String, Comparable<Boolean>>();
        PlcConnection connection = null;
        boolean invalidate = false;
        try {
            connection = this.connectionPool.borrowConnection();
            PlcReadRequest.Builder builder = connection.readRequestBuilder();
            for (String tagCode : tagCodes) {
                TagDef tag = this.tags.get(tagCode);
                if (tag == null) continue;
                builder.addItem(tag.code, tag.address);
            }
            PlcReadResponse response = (PlcReadResponse)builder.build().execute().get(5L, TimeUnit.SECONDS);
            block15: for (String tagCode : tagCodes) {
                TagDef tag = this.tags.get(tagCode);
                if (tag == null) continue;
                if (response.getResponseCode(tag.code) != PlcResponseCode.OK) {
                    values.put(tag.code, null);
                    continue;
                }
                switch (tag.type) {
                    case BOOL: {
                        values.put(tag.code, response.getBoolean(tag.code));
                        continue block15;
                    }
                    case REAL: {
                        values.put(tag.code, response.getFloat(tag.code));
                        continue block15;
                    }
                    case INT: {
                        values.put(tag.code, response.getInteger(tag.code));
                        continue block15;
                    }
                }
                values.put(tag.code, null);
            }
            LinkedHashMap<String, Comparable<Boolean>> linkedHashMap = values;
            return linkedHashMap;
        }
        catch (Exception ex) {
            invalidate = true;
            throw new IllegalStateException("Failed to read H1 PLC tags: " + ex.getMessage(), ex);
        }
        finally {
            if (connection != null) {
                try {
                    if (invalidate) {
                        this.connectionPool.invalidateConnection(connection);
                    } else {
                        this.connectionPool.returnConnection(connection);
                    }
                }
                catch (Exception exception) {}
            }
        }
    }

    private boolean writeBoolTag(String tagCode, boolean value) {
        TagDef tag = this.requireWritableTag(tagCode, TagType.BOOL);
        PlcConnection connection = null;
        try {
            boolean success;
            connection = this.connectionPool.borrowConnection();
            PlcWriteRequest request = connection.writeRequestBuilder().addItem(tag.code, tag.address, new Object[]{value}).build();
            PlcWriteResponse response = (PlcWriteResponse)request.execute().get(5L, TimeUnit.SECONDS);
            boolean bl = success = response.getResponseCode(tag.code) == PlcResponseCode.OK;
            if (success) {
                this.overviewCache.invalidate();
            }
            boolean bl2 = success;
            return bl2;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to write BOOL tag " + tagCode + ": " + ex.getMessage(), ex);
        }
        finally {
            if (connection != null) {
                this.connectionPool.returnConnection(connection);
            }
        }
    }

    private boolean writeRealTag(String tagCode, float value) {
        TagDef tag = this.requireWritableTag(tagCode, TagType.REAL);
        PlcConnection connection = null;
        try {
            boolean success;
            connection = this.connectionPool.borrowConnection();
            PlcWriteRequest request = connection.writeRequestBuilder().addItem(tag.code, tag.address, new Object[]{Float.valueOf(value)}).build();
            PlcWriteResponse response = (PlcWriteResponse)request.execute().get(5L, TimeUnit.SECONDS);
            boolean bl = success = response.getResponseCode(tag.code) == PlcResponseCode.OK;
            if (success) {
                this.overviewCache.invalidate();
            }
            boolean bl2 = success;
            return bl2;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to write REAL tag " + tagCode + ": " + ex.getMessage(), ex);
        }
        finally {
            if (connection != null) {
                this.connectionPool.returnConnection(connection);
            }
        }
    }

    private boolean writeIntTag(String tagCode, int value) {
        TagDef tag = this.requireWritableTag(tagCode, TagType.INT);
        PlcConnection connection = null;
        try {
            boolean success;
            connection = this.connectionPool.borrowConnection();
            PlcWriteRequest request = connection.writeRequestBuilder().addItem(tag.code, tag.address, new Object[]{value}).build();
            PlcWriteResponse response = (PlcWriteResponse)request.execute().get(5L, TimeUnit.SECONDS);
            boolean bl = success = response.getResponseCode(tag.code) == PlcResponseCode.OK;
            if (success) {
                this.overviewCache.invalidate();
            }
            boolean bl2 = success;
            return bl2;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to write INT tag " + tagCode + ": " + ex.getMessage(), ex);
        }
        finally {
            if (connection != null) {
                this.connectionPool.returnConnection(connection);
            }
        }
    }

    private boolean pulseBoolTag(String tagCode, long pulseMs) {
        long duration;
        long l = duration = pulseMs > 0L ? pulseMs : 300L;
        if (!this.writeBoolTag(tagCode, true)) {
            return false;
        }
        try {
            Thread.sleep(duration);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pulse command interrupted", ex);
        }
        return this.writeBoolTag(tagCode, false);
    }

    private TagDef requireWritableTag(String tagCode, TagType expectedType) {
        TagDef tag = this.tags.get(tagCode);
        if (tag == null) {
            throw new IllegalArgumentException("Unknown H1 tag: " + tagCode);
        }
        if (!tag.writable) {
            throw new IllegalArgumentException("Tag is read-only: " + tagCode);
        }
        if (tag.type != expectedType) {
            throw new IllegalArgumentException("Tag type mismatch for " + tagCode);
        }
        return tag;
    }

    private void initDefinitions() {
        for (int i = 1; i <= 10; ++i) {
            this.registerValve("H1_QM" + i, i + "#\u66dd\u6c14\u7ba1\u9600\u95e8", 1, (i - 1) * 2);
        }
        this.registerMotorWithFrequency("H1_GP5", "1#\u6c61\u6ce5\u56de\u6d41\u6cf5", "pump", 20, 22, 26);
        this.registerMotorWithFrequency("H1_GP6", "2#\u6c61\u6ce5\u56de\u6d41\u6cf5", "pump", 30, 32, 36);
        this.registerMotorWithFrequency("H1_GP7", "3#\u6c61\u6ce5\u56de\u6d41\u6cf5", "pump", 40, 42, 46);
        this.registerMotorWithFrequency("H1_GP8", "1#\u6c61\u6ce5\u5269\u4f59\u6cf5", "pump", 50, 52, 56);
        this.registerMotorWithFrequency("H1_GP9", "2#\u6c61\u6ce5\u5269\u4f59\u6cf5", "pump", 60, 62, 66);
        this.registerMotorNoFrequency("H1_HW1", "1#\u9009\u62e9\u533a\u6405\u62cc\u673a", "mixer", 70);
        this.registerMotorNoFrequency("H1_HW2", "2#\u9009\u62e9\u533a\u6405\u62cc\u673a", "mixer", 72);
        this.registerMotorNoFrequency("H1_HW3", "1#\u538c\u6c27\u533a\u6405\u62cc\u673a", "mixer", 74);
        this.registerMotorNoFrequency("H1_HW4", "2#\u538c\u6c27\u533a\u6405\u62cc\u673a", "mixer", 76);
        this.registerMotorNoFrequency("H1_HW5", "3#\u9009\u62e9\u533a\u6405\u62cc\u673a", "mixer", 78);
        this.registerMotorNoFrequency("H1_HW6", "4#\u9009\u62e9\u533a\u6405\u62cc\u673a", "mixer", 80);
        this.registerMotorNoFrequency("H1_HW7", "5#\u9009\u62e9\u533a\u6405\u62cc\u673a", "mixer", 82);
        this.registerMotorNoFrequency("H1_HW8", "6#\u9009\u62e9\u533a\u6405\u62cc\u673a", "mixer", 84);
        this.registerMotorNoFrequency("H1_GP1", "1#\u6c61\u6c34\u56de\u6d41\u6cf5", "pump", 86);
        this.registerMotorNoFrequency("H1_GP2", "2#\u6c61\u6c34\u56de\u6d41\u6cf5", "pump", 88);
        this.registerMotorNoFrequency("H1_GP3", "3#\u6c61\u6c34\u56de\u6d41\u6cf5", "pump", 90);
        this.registerMotorNoFrequency("H1_GP4", "4#\u6c61\u6c34\u56de\u6d41\u6cf5", "pump", 92);
        this.registerInstrument("H1_AIT106", "1#\u6eb6\u89e3\u6c27", 2, 4, 8, 12, 16);
        this.registerInstrument("H1_AIT108", "2#\u6eb6\u89e3\u6c27", 2, 24, 28, 32, 36);
        this.registerInstrument("H1_AIT206", "3#\u6eb6\u89e3\u6c27", 2, 44, 48, 52, 56);
        this.registerInstrument("H1_AIT208", "4#\u6eb6\u89e3\u6c27", 2, 64, 68, 72, 76);
        this.registerInstrument("H1_AIT104", "1#\u785d\u6c2e\u5206\u6790\u4eea", 2, 84, 88, 92, 96);
        this.registerInstrument("H1_AIT110", "2#\u785d\u6c2e\u5206\u6790\u4eea", 2, 104, 108, 112, 116);
        this.registerInstrument("H1_AIT204", "3#\u785d\u6c2e\u5206\u6790\u4eea", 2, 124, 128, 132, 136);
        this.registerInstrument("H1_AIT210", "4#\u785d\u6c2e\u5206\u6790\u4eea", 2, 144, 148, 152, 156);
        this.registerInstrument("H1_AIT103", "5#\u785d\u6c2e\u5206\u6790\u4eea", 2, 164, 168, 172, 176);
        this.registerInstrument("H1_AIT109", "6#\u785d\u6c2e\u5206\u6790\u4eea", 2, 184, 188, 192, 196);
        this.registerInstrument("H1_AIT203", "7#\u785d\u6c2e\u5206\u6790\u4eea", 2, 204, 208, 212, 216);
        this.registerInstrument("H1_AIT209", "8#\u785d\u6c2e\u5206\u6790\u4eea", 2, 224, 228, 232, 236);
        this.registerInstrument("H1_AIT102", "1#\u6c27\u5316\u8fd8\u539f\u7535\u4f4d", 2, 244, 248, 252, 256);
        this.registerInstrument("H1_AIT105", "2#\u6c27\u5316\u8fd8\u539f\u7535\u4f4d", 2, 264, 268, 272, 276);
        this.registerInstrument("H1_AIT202", "3#\u6c27\u5316\u8fd8\u539f\u7535\u4f4d", 2, 284, 288, 292, 296);
        this.registerInstrument("H1_AIT205", "4#\u6c27\u5316\u8fd8\u539f\u7535\u4f4d", 2, 304, 308, 312, 316);
        this.registerInstrument("H1_AIT101", "1#\u6df7\u5408\u6db2\u6d53\u5ea6\u4eea", 2, 324, 328, 332, 336);
        this.registerInstrument("H1_AIT107", "2#\u6df7\u5408\u6db2\u6d53\u5ea6\u4eea", 2, 344, 348, 352, 356);
        this.registerInstrument("H1_AIT201", "3#\u6df7\u5408\u6db2\u6d53\u5ea6\u4eea", 2, 364, 368, 372, 376);
        this.registerInstrument("H1_AIT207", "4#\u6df7\u5408\u6db2\u6d53\u5ea6\u4eea", 2, 384, 388, 392, 396);
        this.registerInstrument("H1_LIT05", "\u8d85\u58f0\u6ce2\u6db2\u4f4d\u8ba1", 2, 444, 448, 452, 456);
        this.registerInstrument("H1_LIT01", "1#\u6c61\u6ce5\u754c\u9762\u4eea", 2, 464, 468, 472, 476);
        this.registerInstrument("H1_LIT02", "2#\u6c61\u6ce5\u754c\u9762\u4eea", 2, 484, 488, 492, 496);
        this.registerInstrument("H1_LIT03", "3#\u6c61\u6ce5\u754c\u9762\u4eea", 2, 504, 508, 512, 516);
        this.registerInstrument("H1_LIT04", "4#\u6c61\u6ce5\u754c\u9762\u4eea", 2, 524, 528, 532, 536);
        this.registerInstrument("H1_AIT301", "\u6c61\u6ce5\u6d53\u5ea6\u8ba1", 2, 544, 548, 552, 556);
        this.registerInstrument("H1_FIT301", "1#\u7535\u78c1\u6d41\u91cf\u8ba1", 2, 584, 588, 592, 596);
        this.registerInstrument("H1_FIT302", "2#\u7535\u78c1\u6d41\u91cf\u8ba1", 2, 604, 608, 612, 616);
        this.registerInstrument("H6_FIQT01", "\u70ed\u8d28\u6d41\u91cf\u8ba1", 2, 624, 628, 632, 636);
        this.registerInstrument("H6_PIT01", "\u538b\u529b\u53d8\u9001\u5668", 2, 644, 648, 652, 656);
        this.registerInstrument("H6_TTT01", "\u6e29\u5ea6\u53d8\u9001\u5668", 2, 664, 668, 672, 676);
        this.registerInstrument("H1_FIT_A20", "A20\u6c60\u8fdb\u6c34\u6d41\u91cf\u8ba1", 2, 684, 688, 692, 696);
        this.registerInstrument("H1_FIT_DISC_2", "\u4e8c\u671f\u8f6c\u76d8\u8fdb\u6c34\u6d41\u91cf\u8ba1", 2, 704, 708, 712, 716);
        this.registerScalar("H1_H2S", "\u786b\u5316\u6c22", 2, 720);
        this.registerTimer("H1_TIMER_HW12", "H1-HW1~2 \u8054\u52a8\u53c2\u6570", 0);
        this.registerTimer("H1_TIMER_HW34", "H1-HW3~4 \u8054\u52a8\u53c2\u6570", 24);
        this.registerTimer("H1_TIMER_HW58", "H1-HW5~8 \u8054\u52a8\u53c2\u6570", 48);
        this.registerTimer("H1_TIMER_GP14", "H1-GP1~4 \u8054\u52a8\u53c2\u6570", 72);
        this.registerTimer("H1_TIMER_QM1_10", "H1-QM1~10 \u8054\u52a8\u53c2\u6570", 96);
        this.registerTimer("H1_TIMER_GP57", "H1-GP5~7 \u8054\u52a8\u53c2\u6570", 120);
        this.registerLogicConfig("H1_GP57_PROTECT_LEVEL", "H1-GP5~7 \u4fdd\u62a4\u6db2\u4f4d", 144, null);
        this.registerLogicConfig("H1_GP89_LEVEL", "H1-GP8~9 \u6db2\u4f4d\u8054\u52a8", 148, 152);
    }

    private void registerValve(String code, String name, int dbNumber, int baseByte) {
        String remote = this.boolTag(code + "_REMOTE", name + "\u8fdc\u7a0b", dbNumber, baseByte, 0, false);
        String openPos = this.boolTag(code + "_OPEN_POS", name + "\u5f00\u5230\u4f4d", dbNumber, baseByte, 1, false);
        String closePos = this.boolTag(code + "_CLOSE_POS", name + "\u5173\u5230\u4f4d", dbNumber, baseByte, 2, false);
        String fault = this.boolTag(code + "_FAULT", name + "\u6545\u969c", dbNumber, baseByte, 3, false);
        String manual = this.boolTag(code + "_MANUAL_MODE", name + "\u624b\u81ea\u52a8", dbNumber, baseByte, 4, true);
        String open = this.boolTag(code + "_OPEN_CMD", name + "\u624b\u52a8\u5f00\u9600", dbNumber, baseByte, 5, true);
        String close = this.boolTag(code + "_CLOSE_CMD", name + "\u624b\u52a8\u5173\u9600", dbNumber, baseByte, 6, true);
        this.devices.put(code, new DeviceDef(code, name, "valve", remote, null, fault, openPos, closePos, manual, open, close, null, null, null));
    }

    private void registerMotorWithFrequency(String code, String name, String type, int boolBaseByte, int feedbackByte, int setpointByte) {
        String remote = this.boolTag(code + "_REMOTE", name + "\u8fdc\u7a0b", 1, boolBaseByte, 0, false);
        String running = this.boolTag(code + "_RUNNING", name + "\u8fd0\u884c", 1, boolBaseByte, 1, false);
        String fault = this.boolTag(code + "_FAULT", name + "\u6545\u969c", 1, boolBaseByte, 2, false);
        String manual = this.boolTag(code + "_MANUAL_MODE", name + "\u624b\u81ea\u52a8", 1, boolBaseByte, 3, true);
        String startStop = this.boolTag(code + "_START_STOP", name + "\u624b\u52a8\u542f\u505c", 1, boolBaseByte, 4, true);
        String feedback = this.realTag(code + "_FREQ_FB", name + "\u9891\u7387\u53cd\u9988", 1, feedbackByte, false);
        String setpoint = this.realTag(code + "_FREQ_SET", name + "\u9891\u7387\u7ed9\u5b9a", 1, setpointByte, true);
        this.devices.put(code, new DeviceDef(code, name, type, remote, running, fault, null, null, manual, null, null, startStop, feedback, setpoint));
    }

    private void registerMotorNoFrequency(String code, String name, String type, int boolBaseByte) {
        String remote = this.boolTag(code + "_REMOTE", name + "\u8fdc\u7a0b", 1, boolBaseByte, 0, false);
        String running = this.boolTag(code + "_RUNNING", name + "\u8fd0\u884c", 1, boolBaseByte, 1, false);
        String fault = this.boolTag(code + "_FAULT", name + "\u6545\u969c", 1, boolBaseByte, 2, false);
        String manual = this.boolTag(code + "_MANUAL_MODE", name + "\u624b\u81ea\u52a8", 1, boolBaseByte, 3, true);
        String startStop = this.boolTag(code + "_START_STOP", name + "\u624b\u52a8\u542f\u505c", 1, boolBaseByte, 4, true);
        this.devices.put(code, new DeviceDef(code, name, type, remote, running, fault, null, null, manual, null, null, startStop, null, null));
    }

    private void registerInstrument(String code, String name, int dbNumber, int upperByte, int lowerByte, int deviationByte, int outputByte) {
        String upper = this.realTag(code + "_UPPER", name + "\u4e0a\u9650\u503c", dbNumber, upperByte, true);
        String lower = this.realTag(code + "_LOWER", name + "\u4e0b\u9650\u503c", dbNumber, lowerByte, true);
        String deviation = this.realTag(code + "_DEVIATION", name + "\u504f\u5dee\u503c", dbNumber, deviationByte, true);
        String output = this.realTag(code + "_OUTPUT", name + "\u8f93\u51fa\u503c", dbNumber, outputByte, false);
        this.instruments.put(code, new InstrumentDef(code, name, upper, lower, deviation, output));
    }

    private void registerTimer(String code, String name, int baseByte) {
        String runElapsedHours = this.intTag(code + "_RUN_ELAPSED_H", name + "\u5df2\u8fd0\u884c\u65f6\u95f4\u65f6", 3, baseByte, false);
        String runElapsedMinutes = this.intTag(code + "_RUN_ELAPSED_M", name + "\u5df2\u8fd0\u884c\u65f6\u95f4\u5206", 3, baseByte + 2, false);
        String runElapsedSeconds = this.intTag(code + "_RUN_ELAPSED_S", name + "\u5df2\u8fd0\u884c\u65f6\u95f4\u79d2", 3, baseByte + 4, false);
        String stopElapsedHours = this.intTag(code + "_STOP_ELAPSED_H", name + "\u5df2\u505c\u6b62\u65f6\u95f4\u65f6", 3, baseByte + 6, false);
        String stopElapsedMinutes = this.intTag(code + "_STOP_ELAPSED_M", name + "\u5df2\u505c\u6b62\u65f6\u95f4\u5206", 3, baseByte + 8, false);
        String stopElapsedSeconds = this.intTag(code + "_STOP_ELAPSED_S", name + "\u5df2\u505c\u6b62\u65f6\u95f4\u79d2", 3, baseByte + 10, false);
        String runSetHours = this.intTag(code + "_RUN_SET_H", name + "\u8fd0\u884c\u8ba1\u65f6\u65f6", 3, baseByte + 12, true);
        String runSetMinutes = this.intTag(code + "_RUN_SET_M", name + "\u8fd0\u884c\u8ba1\u65f6\u5206", 3, baseByte + 14, true);
        String runSetSeconds = this.intTag(code + "_RUN_SET_S", name + "\u8fd0\u884c\u8ba1\u65f6\u79d2", 3, baseByte + 16, true);
        String stopSetHours = this.intTag(code + "_STOP_SET_H", name + "\u505c\u6b62\u8ba1\u65f6\u65f6", 3, baseByte + 18, true);
        String stopSetMinutes = this.intTag(code + "_STOP_SET_M", name + "\u505c\u6b62\u8ba1\u65f6\u5206", 3, baseByte + 20, true);
        String stopSetSeconds = this.intTag(code + "_STOP_SET_S", name + "\u505c\u6b62\u8ba1\u65f6\u79d2", 3, baseByte + 22, true);
        this.timers.put(code, new TimerDef(code, name, runElapsedHours, runElapsedMinutes, runElapsedSeconds, stopElapsedHours, stopElapsedMinutes, stopElapsedSeconds, runSetHours, runSetMinutes, runSetSeconds, stopSetHours, stopSetMinutes, stopSetSeconds));
    }

    private void registerLogicConfig(String code, String name, Integer startLevelByte, Integer stopLevelByte) {
        String startLevel = startLevelByte == null ? null : this.realTag(code + "_START_LEVEL", name + "\u542f\u52a8\u6db2\u4f4d", 3, startLevelByte, true);
        String stopLevel = stopLevelByte == null ? null : this.realTag(code + "_STOP_LEVEL", name + "\u505c\u6b62\u6db2\u4f4d", 3, stopLevelByte, true);
        this.logicConfigs.put(code, new LogicConfigDef(code, name, startLevel, stopLevel));
    }

    private void registerScalar(String code, String name, int dbNumber, int byteOffset) {
        String tagCode = this.realTag(code + "_VALUE", name, dbNumber, byteOffset, false);
        this.communicationValues.put(code, new ScalarDef(code, name, tagCode));
    }

    private String boolTag(String code, String name, int dbNumber, int byteOffset, int bitOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%DB%d.DBX%d.%d:BOOL", dbNumber, byteOffset, bitOffset), TagType.BOOL, writable);
    }

    private String realTag(String code, String name, int dbNumber, int byteOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%DB%d.DBD%d:REAL", dbNumber, byteOffset), TagType.REAL, writable);
    }

    private String intTag(String code, String name, int dbNumber, int byteOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%DB%d.DBW%d:INT", dbNumber, byteOffset), TagType.INT, writable);
    }

    private String registerTag(String code, String name, String address, TagType type, boolean writable) {
        this.tags.put(code, new TagDef(code, name, address, type, writable));
        return code;
    }

    private static void addIfPresent(List<String> list, String value) {
        if (value != null) {
            list.add(value);
        }
    }

    private Map<String, Object> enrichOverview(Map<String, Object> overview, OverviewCache.CacheResult cacheResult) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(overview);
        LinkedHashMap<String, String> plc = (LinkedHashMap<String, String>)result.get("plc");
        if (plc == null) {
            plc = new LinkedHashMap<String, String>();
            result.put("plc", plc);
        }
        plc.put("state", cacheResult.isStale() ? "STALE" : "ONLINE");
        plc.put("stale", (String)((Object)Boolean.valueOf(cacheResult.isStale())));
        plc.put("cacheHit", (String)((Object)Boolean.valueOf(cacheResult.isCacheHit())));
        plc.put("cacheAgeMs", (String)((Object)Long.valueOf(cacheResult.getCacheAgeMs())));
        plc.put("lastSuccessAt", (String)((Object)Long.valueOf(cacheResult.getLastSuccessAt())));
        if (cacheResult.getLastFailureAt() > 0L) {
            plc.put("lastFailureAt", (String)((Object)Long.valueOf(cacheResult.getLastFailureAt())));
            plc.put("lastFailureMessage", cacheResult.getLastFailureMessage());
        }
        return result;
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
        private final String frequencyFeedbackTagCode;
        private final String frequencySetpointTagCode;

        private DeviceDef(String code, String name, String type, String remoteTagCode, String runningTagCode, String faultTagCode, String openPositionTagCode, String closePositionTagCode, String manualModeTagCode, String openCommandTagCode, String closeCommandTagCode, String startStopTagCode, String frequencyFeedbackTagCode, String frequencySetpointTagCode) {
            this.code = code;
            this.name = name;
            this.type = type;
            this.remoteTagCode = remoteTagCode;
            this.runningTagCode = runningTagCode;
            this.faultTagCode = faultTagCode;
            this.openPositionTagCode = openPositionTagCode;
            this.closePositionTagCode = closePositionTagCode;
            this.manualModeTagCode = manualModeTagCode;
            this.openCommandTagCode = openCommandTagCode;
            this.closeCommandTagCode = closeCommandTagCode;
            this.startStopTagCode = startStopTagCode;
            this.frequencyFeedbackTagCode = frequencyFeedbackTagCode;
            this.frequencySetpointTagCode = frequencySetpointTagCode;
        }

        private List<String> getReadTags() {
            ArrayList<String> tagCodes = new ArrayList<String>();
            H1PlcService.addIfPresent(tagCodes, this.remoteTagCode);
            H1PlcService.addIfPresent(tagCodes, this.runningTagCode);
            H1PlcService.addIfPresent(tagCodes, this.faultTagCode);
            H1PlcService.addIfPresent(tagCodes, this.openPositionTagCode);
            H1PlcService.addIfPresent(tagCodes, this.closePositionTagCode);
            H1PlcService.addIfPresent(tagCodes, this.manualModeTagCode);
            H1PlcService.addIfPresent(tagCodes, this.frequencyFeedbackTagCode);
            H1PlcService.addIfPresent(tagCodes, this.frequencySetpointTagCode);
            return tagCodes;
        }
    }

    private static class CommandWrite {
        private final String tagCode;
        private final boolean value;

        private CommandWrite(String tagCode, boolean value) {
            this.tagCode = tagCode;
            this.value = value;
        }
    }

    private static class InstrumentDef {
        private final String code;
        private final String name;
        private final String upperLimitTagCode;
        private final String lowerLimitTagCode;
        private final String deviationTagCode;
        private final String outputTagCode;

        private InstrumentDef(String code, String name, String upperLimitTagCode, String lowerLimitTagCode, String deviationTagCode, String outputTagCode) {
            this.code = code;
            this.name = name;
            this.upperLimitTagCode = upperLimitTagCode;
            this.lowerLimitTagCode = lowerLimitTagCode;
            this.deviationTagCode = deviationTagCode;
            this.outputTagCode = outputTagCode;
        }

        private List<String> getReadTags() {
            return Arrays.asList(this.upperLimitTagCode, this.lowerLimitTagCode, this.deviationTagCode, this.outputTagCode);
        }
    }

    private static class TimerDef {
        private final String code;
        private final String name;
        private final String runHoursElapsedTagCode;
        private final String runMinutesElapsedTagCode;
        private final String runSecondsElapsedTagCode;
        private final String stopHoursElapsedTagCode;
        private final String stopMinutesElapsedTagCode;
        private final String stopSecondsElapsedTagCode;
        private final String runHoursSetTagCode;
        private final String runMinutesSetTagCode;
        private final String runSecondsSetTagCode;
        private final String stopHoursSetTagCode;
        private final String stopMinutesSetTagCode;
        private final String stopSecondsSetTagCode;

        private TimerDef(String code, String name, String runHoursElapsedTagCode, String runMinutesElapsedTagCode, String runSecondsElapsedTagCode, String stopHoursElapsedTagCode, String stopMinutesElapsedTagCode, String stopSecondsElapsedTagCode, String runHoursSetTagCode, String runMinutesSetTagCode, String runSecondsSetTagCode, String stopHoursSetTagCode, String stopMinutesSetTagCode, String stopSecondsSetTagCode) {
            this.code = code;
            this.name = name;
            this.runHoursElapsedTagCode = runHoursElapsedTagCode;
            this.runMinutesElapsedTagCode = runMinutesElapsedTagCode;
            this.runSecondsElapsedTagCode = runSecondsElapsedTagCode;
            this.stopHoursElapsedTagCode = stopHoursElapsedTagCode;
            this.stopMinutesElapsedTagCode = stopMinutesElapsedTagCode;
            this.stopSecondsElapsedTagCode = stopSecondsElapsedTagCode;
            this.runHoursSetTagCode = runHoursSetTagCode;
            this.runMinutesSetTagCode = runMinutesSetTagCode;
            this.runSecondsSetTagCode = runSecondsSetTagCode;
            this.stopHoursSetTagCode = stopHoursSetTagCode;
            this.stopMinutesSetTagCode = stopMinutesSetTagCode;
            this.stopSecondsSetTagCode = stopSecondsSetTagCode;
        }

        private List<String> getReadTags() {
            return Arrays.asList(this.runHoursElapsedTagCode, this.runMinutesElapsedTagCode, this.runSecondsElapsedTagCode, this.stopHoursElapsedTagCode, this.stopMinutesElapsedTagCode, this.stopSecondsElapsedTagCode, this.runHoursSetTagCode, this.runMinutesSetTagCode, this.runSecondsSetTagCode, this.stopHoursSetTagCode, this.stopMinutesSetTagCode, this.stopSecondsSetTagCode);
        }
    }

    private static class LogicConfigDef {
        private final String code;
        private final String name;
        private final String startLevelTagCode;
        private final String stopLevelTagCode;

        private LogicConfigDef(String code, String name, String startLevelTagCode, String stopLevelTagCode) {
            this.code = code;
            this.name = name;
            this.startLevelTagCode = startLevelTagCode;
            this.stopLevelTagCode = stopLevelTagCode;
        }

        private List<String> getReadTags() {
            ArrayList<String> tagCodes = new ArrayList<String>();
            H1PlcService.addIfPresent(tagCodes, this.startLevelTagCode);
            H1PlcService.addIfPresent(tagCodes, this.stopLevelTagCode);
            return tagCodes;
        }
    }

    private static class TagDef {
        private final String code;
        private final String name;
        private final String address;
        private final TagType type;
        private final boolean writable;

        private TagDef(String code, String name, String address, TagType type, boolean writable) {
            this.code = code;
            this.name = name;
            this.address = address;
            this.type = type;
            this.writable = writable;
        }
    }

    private static enum TagType {
        BOOL,
        REAL,
        INT;

    }

    private static class ScalarDef {
        private final String code;
        private final String name;
        private final String tagCode;

        private ScalarDef(String code, String name, String tagCode) {
            this.code = code;
            this.name = name;
            this.tagCode = tagCode;
        }
    }
}

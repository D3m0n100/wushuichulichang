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

import com.example.waterfactoryceshi.service.M3PlcConnectionPool;
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
public class M3PlcService {
    private static final Logger log = LoggerFactory.getLogger(M3PlcService.class);
    private final M3PlcConnectionPool connectionPool;
    private final OverviewCache overviewCache = new OverviewCache();
    @Value(value="${plc.m3.ip}")
    private String plcIp;
    @Value(value="${plc.m3.rack:0}")
    private int rack;
    @Value(value="${plc.m3.slot:1}")
    private int slot;
    private final Map<String, TagDef> tags = new LinkedHashMap<String, TagDef>();
    private final Map<String, DeviceDef> devices = new LinkedHashMap<String, DeviceDef>();
    private final Map<String, InstrumentDef> instruments = new LinkedHashMap<String, InstrumentDef>();
    private final Map<String, TimerDef> timers = new LinkedHashMap<String, TimerDef>();
    private final Map<String, LogicConfigDef> logicConfigs = new LinkedHashMap<String, LogicConfigDef>();

    public M3PlcService(M3PlcConnectionPool connectionPool) {
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
            result.put("gas", this.readGasValues());
            result.put("parameters", this.readParameters());
            result.put("catalogDevices", this.catalogDevices());
            this.overviewCache.set(result);
        }
        catch (Exception ex) {
            this.overviewCache.markFailure(ex.getMessage());
            log.warn("M3 PLC background fetch failed: {}", (Object)ex.getMessage());
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
        DeviceDef device = this.devices.get(deviceCode);
        if (device == null) {
            throw new IllegalArgumentException("Unknown M3 device: " + deviceCode);
        }
        return this.buildDeviceState(device, this.readTags(device.getReadTags()));
    }

    public boolean writeManualMode(String deviceCode, boolean enabled) {
        DeviceDef device = this.requireDevice(deviceCode);
        if (device.manualModeTagCode == null) {
            throw new IllegalArgumentException("Device does not support manual mode: " + deviceCode);
        }
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

    public boolean writeLogicConfig(String configCode, Float startLevel, Float stopLevel, Integer delayMinutes, Integer delaySeconds) {
        LogicConfigDef config = this.logicConfigs.get(configCode);
        if (config == null) {
            throw new IllegalArgumentException("Unknown logic config: " + configCode);
        }
        boolean success = true;
        if (startLevel != null && config.startLevelTagCode != null) {
            boolean bl = success = success && this.writeRealTag(config.startLevelTagCode, startLevel.floatValue());
        }
        if (stopLevel != null && config.stopLevelTagCode != null) {
            boolean bl = success = success && this.writeRealTag(config.stopLevelTagCode, stopLevel.floatValue());
        }
        if (delayMinutes != null && config.delayMinutesTagCode != null) {
            boolean bl = success = success && this.writeIntTag(config.delayMinutesTagCode, delayMinutes);
        }
        if (delaySeconds != null && config.delaySecondsTagCode != null) {
            success = success && this.writeIntTag(config.delaySecondsTagCode, delaySeconds);
        }
        return success;
    }

    public boolean writeParameter(String parameterCode, String value) {
        TagDef tag = this.tags.get(parameterCode);
        if (tag == null) {
            throw new IllegalArgumentException("Unknown M3 parameter: " + parameterCode);
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
        meta.put("name", "M3");
        meta.put("series", "S7-1200");
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
            if (config.startLevelTagCode != null) {
                item.put("startLevel", values.get(config.startLevelTagCode));
            }
            if (config.stopLevelTagCode != null) {
                item.put("stopLevel", values.get(config.stopLevelTagCode));
            }
            if (config.delayMinutesTagCode != null) {
                item.put("delayMinutes", values.get(config.delayMinutesTagCode));
            }
            if (config.delaySecondsTagCode != null) {
                item.put("delaySeconds", values.get(config.delaySecondsTagCode));
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> readGasValues() {
        String[] gasTags = new String[]{"M3_GAS_H2S", "M3_GAS_CO", "M3_GAS_CH4", "M3_GAS_O2"};
        Map<String, Object> values = this.readTags(this.asList(gasTags));
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("name", "\u6c14\u4f53\u591a\u53c2\u6570");
        item.put("h2s", values.get("M3_GAS_H2S"));
        item.put("co", values.get("M3_GAS_CO"));
        item.put("ch4", values.get("M3_GAS_CH4"));
        item.put("o2", values.get("M3_GAS_O2"));
        return item;
    }

    private List<Map<String, Object>> catalogParameters() {
        LinkedHashMap<String, Map<String, Object>> ordered = new LinkedHashMap<String, Map<String, Object>>();
        this.appendInstrumentParameters(ordered);
        this.appendTimerParameters(ordered);
        this.appendLogicParameters(ordered);
        this.appendGasParameters(ordered);
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

    private void appendGasParameters(Map<String, Map<String, Object>> ordered) {
        this.addParameterTag(ordered, "M3_GAS_H2S", "gas", "M3_GAS", "gas");
        this.addParameterTag(ordered, "M3_GAS_CO", "gas", "M3_GAS", "gas");
        this.addParameterTag(ordered, "M3_GAS_CH4", "gas", "M3_GAS", "gas");
        this.addParameterTag(ordered, "M3_GAS_O2", "gas", "M3_GAS", "gas");
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
            throw new IllegalArgumentException("Unknown M3 device: " + deviceCode);
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
            throw new IllegalStateException("Failed to read M3 PLC tags: " + ex.getMessage(), ex);
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
            throw new IllegalArgumentException("Unknown M3 tag: " + tagCode);
        }
        if (!tag.writable) {
            throw new IllegalArgumentException("Tag is read-only: " + tagCode);
        }
        if (tag.type != expectedType) {
            throw new IllegalArgumentException("Tag type mismatch for " + tagCode);
        }
        return tag;
    }

    private List<String> asList(String ... tagCodes) {
        return Arrays.asList(tagCodes);
    }

    private void initDefinitions() {
        this.registerValve("M3_QM1", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u7535\u52a8\u95f8\u677f", 0);
        this.registerValve("P2_QM1", "\u6d88\u6bd2\u6c60\u4e2d\u6c34\u9600\u95e8", 2);
        this.registerMotorWithFrequency("M3_GP1", "1#\u56de\u6d41\u6c61\u6ce5\u8f6c\u5b50\u6cf5", "pump", 4, 6, 10);
        this.registerMotorWithFrequency("M3_GP2", "2#\u56de\u6d41\u6c61\u6ce5\u8f6c\u5b50\u6cf5", "pump", 14, 16, 20);
        this.registerMotorWithFrequency("M3_GP3", "\u5269\u4f59\u6c61\u6ce5\u8f6c\u5b50\u6cf5", "pump", 24, 26, 30);
        this.registerMotorWithFrequency("M3_HW1", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u7d6e\u51dd\u6405\u62cc\u673a", "mixer", 34, 36, 40);
        this.registerMotorWithFrequency("M3_HW2", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u6df7\u5408\u6405\u62cc\u673a", "mixer", 44, 46, 50);
        this.registerMotorNoFrequency("M3_GP4", "\u96c6\u6c34\u5751\u6f5c\u6c61\u6cf5", "pump", 54);
        this.registerMotorNoFrequency("M3_HL1", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u522e\u6ce5\u673a", "scraper", 56);
        this.registerMotorNoFrequency("P2_GP1", "\u6d88\u6bd2\u6c60\u4e2d\u6c34\u6c34\u6cf5", "pump", 58);
        this.registerInstrument("P2_AIT01", "\u6d88\u6bd2\u6c60\u4f59\u6c2f", 2, 4, 8, 12, 1, 60);
        this.registerInstrument("M3_RIT01", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u6c61\u6ce5\u754c\u9762\u4eea", 2, 24, 28, 32, 2, 36);
        this.registerInstrument("M3_LIT01", "\u9ad8\u6548\u6c89\u6dc0\u6c601#\u8d85\u58f0\u6ce2\u6db2\u4f4d\u8ba1", 2, 44, 48, 52, 2, 56);
        this.registerInstrument("M3_LIT02", "\u9ad8\u6548\u6c89\u6dc0\u6c602#\u8d85\u58f0\u6ce2\u6db2\u4f4d\u8ba1", 2, 64, 68, 72, 2, 76);
        this.registerInstrument("M3_AIT01", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u6d4a\u5ea6\u8ba1", 2, 84, 88, 92, 2, 96);
        this.registerInstrument("M3_FIT01", "\u4e00\u671f\u8f6c\u76d8\u8fdb\u6c34\u6d41\u91cf\u8ba1", 2, 104, 108, 112, 2, 116);
        this.registerInstrument("M3_FIT02", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u5269\u4f59\u6c61\u6ce5\u6d41\u91cf\u8ba1", 2, 124, 128, 132, 2, 136);
        this.registerTag("M3_GAS_H2S", "\u786b\u5316\u6c22", this.dbRealAddress(2, 140), TagType.REAL, false);
        this.registerTag("M3_GAS_CO", "\u4e00\u6c27\u5316\u78b3", this.dbRealAddress(2, 144), TagType.REAL, false);
        this.registerTag("M3_GAS_CH4", "\u7532\u70f7", this.dbRealAddress(2, 148), TagType.REAL, false);
        this.registerTag("M3_GAS_O2", "\u6c27\u6c14", this.dbRealAddress(2, 152), TagType.REAL, false);
        this.registerTimer("M3_TIMER_HL1", "\u9ad8\u6548\u6c89\u6dc0\u6c60\u522e\u6ce5\u673a\u8054\u52a8\u53c2\u6570", 0);
        this.registerTimer("M3_TIMER_GP12", "M3-GP1~2 \u8054\u52a8\u53c2\u6570", 24);
        this.registerTimer("M3_TIMER_GP3", "M3-GP3 \u8054\u52a8\u53c2\u6570", 48);
        this.registerLogicConfig("M3_GP3_LEVEL", "M3-GP3 \u6db2\u4f4d\u8054\u52a8", 72, 76, null, null);
        this.registerLogicConfig("P2_GP1_LEVEL_DELAY", "P2-GP1 \u6db2\u4f4d\u4e0e\u5ef6\u65f6\u8054\u52a8", 80, 84, 88, 90);
    }

    private void registerValve(String code, String name, int baseByte) {
        String remote = this.registerTag(code + "_REMOTE", name + "\u8fdc\u7a0b", this.dbBoolAddress(1, baseByte, 0), TagType.BOOL, false);
        String openPos = this.registerTag(code + "_OPEN_POS", name + "\u5f00\u5230\u4f4d", this.dbBoolAddress(1, baseByte, 1), TagType.BOOL, false);
        String closePos = this.registerTag(code + "_CLOSE_POS", name + "\u5173\u5230\u4f4d", this.dbBoolAddress(1, baseByte, 2), TagType.BOOL, false);
        String fault = this.registerTag(code + "_FAULT", name + "\u6545\u969c", this.dbBoolAddress(1, baseByte, 3), TagType.BOOL, false);
        String manual = this.registerTag(code + "_MANUAL_MODE", name + "\u624b\u81ea\u52a8", this.dbBoolAddress(1, baseByte, 4), TagType.BOOL, true);
        String open = this.registerTag(code + "_OPEN_CMD", name + "\u624b\u52a8\u5f00\u9600", this.dbBoolAddress(1, baseByte, 5), TagType.BOOL, true);
        String close = this.registerTag(code + "_CLOSE_CMD", name + "\u624b\u52a8\u5173\u9600", this.dbBoolAddress(1, baseByte, 6), TagType.BOOL, true);
        this.devices.put(code, new DeviceDef(code, name, "valve", remote, null, fault, openPos, closePos, manual, open, close, null, null, null));
    }

    private void registerMotorWithFrequency(String code, String name, String type, int boolBaseByte, int feedbackByte, int setpointByte) {
        String remote = this.registerTag(code + "_REMOTE", name + "\u8fdc\u7a0b", this.dbBoolAddress(1, boolBaseByte, 0), TagType.BOOL, false);
        String running = this.registerTag(code + "_RUNNING", name + "\u8fd0\u884c", this.dbBoolAddress(1, boolBaseByte, 1), TagType.BOOL, false);
        String fault = this.registerTag(code + "_FAULT", name + "\u6545\u969c", this.dbBoolAddress(1, boolBaseByte, 2), TagType.BOOL, false);
        String manual = this.registerTag(code + "_MANUAL_MODE", name + "\u624b\u81ea\u52a8", this.dbBoolAddress(1, boolBaseByte, 3), TagType.BOOL, true);
        String command = this.registerTag(code + "_START_STOP", name + "\u624b\u52a8\u542f\u505c", this.dbBoolAddress(1, boolBaseByte, 4), TagType.BOOL, true);
        String feedback = this.registerTag(code + "_FREQ_FB", name + "\u9891\u7387\u53cd\u9988", this.dbRealAddress(1, feedbackByte), TagType.REAL, false);
        String setpoint = this.registerTag(code + "_FREQ_SET", name + "\u9891\u7387\u7ed9\u5b9a", this.dbRealAddress(1, setpointByte), TagType.REAL, true);
        this.devices.put(code, new DeviceDef(code, name, type, remote, running, fault, null, null, manual, null, null, command, feedback, setpoint));
    }

    private void registerMotorNoFrequency(String code, String name, String type, int boolBaseByte) {
        String remote = this.registerTag(code + "_REMOTE", name + "\u8fdc\u7a0b", this.dbBoolAddress(1, boolBaseByte, 0), TagType.BOOL, false);
        String running = this.registerTag(code + "_RUNNING", name + "\u8fd0\u884c", this.dbBoolAddress(1, boolBaseByte, 1), TagType.BOOL, false);
        String fault = this.registerTag(code + "_FAULT", name + "\u6545\u969c", this.dbBoolAddress(1, boolBaseByte, 2), TagType.BOOL, false);
        String manual = this.registerTag(code + "_MANUAL_MODE", name + "\u624b\u81ea\u52a8", this.dbBoolAddress(1, boolBaseByte, 3), TagType.BOOL, true);
        String command = this.registerTag(code + "_START_STOP", name + "\u624b\u52a8\u542f\u505c", this.dbBoolAddress(1, boolBaseByte, 4), TagType.BOOL, true);
        this.devices.put(code, new DeviceDef(code, name, type, remote, running, fault, null, null, manual, null, null, command, null, null));
    }

    private void registerInstrument(String code, String name, int dbNumber, int upperByte, int lowerByte, int deviationByte, int outputDbNumber, int outputByte) {
        String upper = this.registerTag(code + "_UPPER", name + "\u4e0a\u9650\u503c", this.dbRealAddress(dbNumber, upperByte), TagType.REAL, true);
        String lower = this.registerTag(code + "_LOWER", name + "\u4e0b\u9650\u503c", this.dbRealAddress(dbNumber, lowerByte), TagType.REAL, true);
        String deviation = this.registerTag(code + "_DEVIATION", name + "\u504f\u5dee\u503c", this.dbRealAddress(dbNumber, deviationByte), TagType.REAL, true);
        String output = this.registerTag(code + "_OUTPUT", name + "\u8f93\u51fa\u503c", this.dbRealAddress(outputDbNumber, outputByte), TagType.REAL, false);
        this.instruments.put(code, new InstrumentDef(code, name, upper, lower, deviation, output));
    }

    private void registerTimer(String code, String name, int baseByte) {
        String runElapsedHours = this.registerTag(code + "_RUN_ELAPSED_H", name + "\u5df2\u8fd0\u884c\u65f6\u95f4\u65f6", this.dbIntAddress(3, baseByte), TagType.INT, false);
        String runElapsedMinutes = this.registerTag(code + "_RUN_ELAPSED_M", name + "\u5df2\u8fd0\u884c\u65f6\u95f4\u5206", this.dbIntAddress(3, baseByte + 2), TagType.INT, false);
        String runElapsedSeconds = this.registerTag(code + "_RUN_ELAPSED_S", name + "\u5df2\u8fd0\u884c\u65f6\u95f4\u79d2", this.dbIntAddress(3, baseByte + 4), TagType.INT, false);
        String stopElapsedHours = this.registerTag(code + "_STOP_ELAPSED_H", name + "\u5df2\u505c\u6b62\u65f6\u95f4\u65f6", this.dbIntAddress(3, baseByte + 6), TagType.INT, false);
        String stopElapsedMinutes = this.registerTag(code + "_STOP_ELAPSED_M", name + "\u5df2\u505c\u6b62\u65f6\u95f4\u5206", this.dbIntAddress(3, baseByte + 8), TagType.INT, false);
        String stopElapsedSeconds = this.registerTag(code + "_STOP_ELAPSED_S", name + "\u5df2\u505c\u6b62\u65f6\u95f4\u79d2", this.dbIntAddress(3, baseByte + 10), TagType.INT, false);
        String runSetHours = this.registerTag(code + "_RUN_SET_H", name + "\u8fd0\u884c\u8ba1\u65f6\u65f6", this.dbIntAddress(3, baseByte + 12), TagType.INT, true);
        String runSetMinutes = this.registerTag(code + "_RUN_SET_M", name + "\u8fd0\u884c\u8ba1\u65f6\u5206", this.dbIntAddress(3, baseByte + 14), TagType.INT, true);
        String runSetSeconds = this.registerTag(code + "_RUN_SET_S", name + "\u8fd0\u884c\u8ba1\u65f6\u79d2", this.dbIntAddress(3, baseByte + 16), TagType.INT, true);
        String stopSetHours = this.registerTag(code + "_STOP_SET_H", name + "\u505c\u6b62\u8ba1\u65f6\u65f6", this.dbIntAddress(3, baseByte + 18), TagType.INT, true);
        String stopSetMinutes = this.registerTag(code + "_STOP_SET_M", name + "\u505c\u6b62\u8ba1\u65f6\u5206", this.dbIntAddress(3, baseByte + 20), TagType.INT, true);
        String stopSetSeconds = this.registerTag(code + "_STOP_SET_S", name + "\u505c\u6b62\u8ba1\u65f6\u79d2", this.dbIntAddress(3, baseByte + 22), TagType.INT, true);
        this.timers.put(code, new TimerDef(code, name, runElapsedHours, runElapsedMinutes, runElapsedSeconds, stopElapsedHours, stopElapsedMinutes, stopElapsedSeconds, runSetHours, runSetMinutes, runSetSeconds, stopSetHours, stopSetMinutes, stopSetSeconds));
    }

    private void registerLogicConfig(String code, String name, Integer startLevelByte, Integer stopLevelByte, Integer delayMinutesByte, Integer delaySecondsByte) {
        String startLevel = startLevelByte == null ? null : this.registerTag(code + "_START_LEVEL", name + "\u542f\u52a8\u6db2\u4f4d", this.dbRealAddress(3, startLevelByte), TagType.REAL, true);
        String stopLevel = stopLevelByte == null ? null : this.registerTag(code + "_STOP_LEVEL", name + "\u505c\u6b62\u6db2\u4f4d", this.dbRealAddress(3, stopLevelByte), TagType.REAL, true);
        String delayMinutes = delayMinutesByte == null ? null : this.registerTag(code + "_DELAY_MIN", name + "\u5ef6\u65f6\u5206", this.dbIntAddress(3, delayMinutesByte), TagType.INT, true);
        String delaySeconds = delaySecondsByte == null ? null : this.registerTag(code + "_DELAY_SEC", name + "\u5ef6\u65f6\u79d2", this.dbIntAddress(3, delaySecondsByte), TagType.INT, true);
        this.logicConfigs.put(code, new LogicConfigDef(code, name, startLevel, stopLevel, delayMinutes, delaySeconds));
    }

    private String registerTag(String code, String name, String address, TagType type, boolean writable) {
        this.tags.put(code, new TagDef(code, name, address, type, writable));
        return code;
    }

    private String dbBoolAddress(int dbNumber, int byteOffset, int bitOffset) {
        return String.format("%%DB%d.DBX%d.%d:BOOL", dbNumber, byteOffset, bitOffset);
    }

    private String dbRealAddress(int dbNumber, int byteOffset) {
        return String.format("%%DB%d.DBD%d:REAL", dbNumber, byteOffset);
    }

    private String dbIntAddress(int dbNumber, int byteOffset) {
        return String.format("%%DB%d.DBW%d:INT", dbNumber, byteOffset);
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
            M3PlcService.addIfPresent(tagCodes, this.remoteTagCode);
            M3PlcService.addIfPresent(tagCodes, this.runningTagCode);
            M3PlcService.addIfPresent(tagCodes, this.faultTagCode);
            M3PlcService.addIfPresent(tagCodes, this.openPositionTagCode);
            M3PlcService.addIfPresent(tagCodes, this.closePositionTagCode);
            M3PlcService.addIfPresent(tagCodes, this.manualModeTagCode);
            M3PlcService.addIfPresent(tagCodes, this.frequencyFeedbackTagCode);
            M3PlcService.addIfPresent(tagCodes, this.frequencySetpointTagCode);
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
        private final String delayMinutesTagCode;
        private final String delaySecondsTagCode;

        private LogicConfigDef(String code, String name, String startLevelTagCode, String stopLevelTagCode, String delayMinutesTagCode, String delaySecondsTagCode) {
            this.code = code;
            this.name = name;
            this.startLevelTagCode = startLevelTagCode;
            this.stopLevelTagCode = stopLevelTagCode;
            this.delayMinutesTagCode = delayMinutesTagCode;
            this.delaySecondsTagCode = delaySecondsTagCode;
        }

        private List<String> getReadTags() {
            ArrayList<String> tagCodes = new ArrayList<String>();
            M3PlcService.addIfPresent(tagCodes, this.startLevelTagCode);
            M3PlcService.addIfPresent(tagCodes, this.stopLevelTagCode);
            M3PlcService.addIfPresent(tagCodes, this.delayMinutesTagCode);
            M3PlcService.addIfPresent(tagCodes, this.delaySecondsTagCode);
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
}

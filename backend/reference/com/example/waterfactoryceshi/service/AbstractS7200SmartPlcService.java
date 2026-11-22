/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.sourceforge.snap7.moka7.S7
 *  com.sourceforge.snap7.moka7.S7Client
 *  javax.annotation.PreDestroy
 *  org.apache.commons.pool2.BasePooledObjectFactory
 *  org.apache.commons.pool2.PooledObject
 *  org.apache.commons.pool2.PooledObjectFactory
 *  org.apache.commons.pool2.impl.DefaultPooledObject
 *  org.apache.commons.pool2.impl.GenericObjectPool
 *  org.apache.commons.pool2.impl.GenericObjectPoolConfig
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.OverviewCache;
import com.example.waterfactoryceshi.service.PlcConnectionProvider;
import com.sourceforge.snap7.moka7.S7;
import com.sourceforge.snap7.moka7.S7Client;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PreDestroy;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractS7200SmartPlcService {
    private static final Logger log = LoggerFactory.getLogger(AbstractS7200SmartPlcService.class);
    private static final Pattern BOOL_AREA_PATTERN = Pattern.compile("%([IQM])(\\d+)\\.(\\d+):BOOL");
    private static final Pattern BOOL_DBX_PATTERN = Pattern.compile("%DB(\\d+)\\.DBX(\\d+)\\.(\\d+):BOOL");
    private static final Pattern INT_AREA_WORD_PATTERN = Pattern.compile("%([IQM])W(\\d+):INT");
    private static final Pattern INT_DBW_PATTERN = Pattern.compile("%DB(\\d+)\\.DBW(\\d+):INT");
    private static final Pattern DINT_DBD_PATTERN = Pattern.compile("%DB(\\d+)\\.DBD(\\d+):DINT");
    private static final Pattern REAL_DBD_PATTERN = Pattern.compile("%DB(\\d+)\\.DBD(\\d+):REAL");
    private static final int MAX_BATCH_GAP_BYTES = 8;
    private static final int MAX_BATCH_SIZE_BYTES = 64;
    private static final long OVERVIEW_CACHE_TTL_MS = 1200L;
    private static final int SMART_CONNECT_TIMEOUT_MS = 5000;
    private static final int[][] SMART_TSAP_CANDIDATES = new int[][]{{256, 513}, {256, 512}, {768, 513}, {768, 512}, {256, 257}, {256, 256}};
    private final String plcName;
    private final String series;
    private final String plcIp;
    private final int rack;
    private final int slot;
    private final GenericObjectPool<S7Client> snap7ClientPool;
    private final OverviewCache overviewCache = new OverviewCache();
    protected final Map<String, TagDef> tags = new LinkedHashMap<String, TagDef>();
    protected final Map<String, DeviceDef> devices = new LinkedHashMap<String, DeviceDef>();
    protected final Map<String, ParameterDef> parameters = new LinkedHashMap<String, ParameterDef>();
    private static final ExecutorService connectExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "snap7-connect");
        t.setDaemon(true);
        return t;
    });

    protected AbstractS7200SmartPlcService(PlcConnectionProvider connectionProvider, String plcName, String series, String plcIp, int rack, int slot) {
        this.plcName = plcName;
        this.series = series;
        this.plcIp = plcIp;
        this.rack = rack;
        this.slot = slot;
        this.snap7ClientPool = this.createClientPool();
        this.initDefinitions();
    }

    protected abstract void initDefinitions();

    public Map<String, Object> readOverview() {
        OverviewCache.CacheResult cacheResult = this.overviewCache.getResilient(1200L, new OverviewCache.Loader(){

            @Override
            public Object load() {
                Map<String, Object> snapshot = AbstractS7200SmartPlcService.this.readTags(new ArrayList<String>(AbstractS7200SmartPlcService.this.tags.keySet()));
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
                result.put("plc", AbstractS7200SmartPlcService.this.buildPlcMeta());
                result.put("devices", AbstractS7200SmartPlcService.this.readDevices(snapshot));
                result.put("parameters", AbstractS7200SmartPlcService.this.readParameters(snapshot));
                return result;
            }
        });
        return this.enrichOverview((Map)cacheResult.getValue(), cacheResult);
    }

    public Map<String, Object> catalog() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("plc", this.buildPlcMeta());
        result.put("devices", this.catalogDevices());
        result.put("parameters", this.catalogParameters());
        result.put("instruments", new ArrayList());
        result.put("timers", new ArrayList());
        result.put("logicConfigs", new ArrayList());
        return result;
    }

    public Map<String, Object> readDevice(String deviceCode) {
        DeviceDef device = this.requireDevice(deviceCode);
        return this.buildDeviceState(device, this.readTags(device.getReadTags()));
    }

    public boolean writeManualMode(String deviceCode, boolean enabled) {
        DeviceDef device = this.requireDevice(deviceCode);
        if (device.manualModeTagCode == null) {
            throw new IllegalArgumentException("Device does not support manual mode: " + deviceCode);
        }
        boolean success = this.writeBoolTag(device.manualModeTagCode, enabled);
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    public boolean pulseCommand(String deviceCode, long pulseMs) {
        DeviceDef device = this.requireDevice(deviceCode);
        if (device.commandTagCode == null) {
            throw new IllegalArgumentException("Device does not support command pulse: " + deviceCode);
        }
        boolean success = this.pulseBoolTag(device.commandTagCode, pulseMs);
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    public boolean writeCommand(String deviceCode, String value) {
        DeviceDef device = this.requireDevice(deviceCode);
        if (device.commandTagCode == null) {
            throw new IllegalArgumentException("Device does not support command: " + deviceCode);
        }
        boolean boolValue = "1".equals(value) || "true".equalsIgnoreCase(value);
        boolean success = this.writeBoolTag(device.commandTagCode, boolValue);
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    public boolean writeParameter(String parameterCode, String value) {
        boolean success;
        ParameterDef parameter = this.parameters.get(parameterCode);
        if (parameter == null) {
            throw new IllegalArgumentException("Unknown parameter: " + parameterCode);
        }
        TagDef tag = this.requireWritableTag(parameter.tagCode);
        switch (tag.type) {
            case BOOL: {
                success = this.writeBoolTag(tag.code, Boolean.parseBoolean(value));
                break;
            }
            case INT: {
                success = this.writeIntTag(tag.code, Integer.parseInt(value));
                break;
            }
            case DINT: {
                success = this.writeDIntTag(tag.code, Integer.parseInt(value));
                break;
            }
            case REAL: {
                success = this.writeRealTag(tag.code, Float.parseFloat(value));
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported parameter type: " + (Object)((Object)tag.type));
            }
        }
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    protected String registerBoolInputTag(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%I%d.%d:BOOL", byteOffset, bitOffset), TagType.BOOL, writable);
    }

    protected String registerBoolOutputTag(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%Q%d.%d:BOOL", byteOffset, bitOffset), TagType.BOOL, writable);
    }

    protected String registerBoolMTag(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%M%d.%d:BOOL", byteOffset, bitOffset), TagType.BOOL, writable);
    }

    protected String registerBoolVTag(String code, String name, int byteOffset, int bitOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%DB1.DBX%d.%d:BOOL", byteOffset, bitOffset), TagType.BOOL, writable);
    }

    protected String registerIntInputWordTag(String code, String name, int byteOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%IW%d:INT", byteOffset), TagType.INT, writable);
    }

    protected String registerIntVTag(String code, String name, int byteOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%DB1.DBW%d:INT", byteOffset), TagType.INT, writable);
    }

    protected String registerDIntVTag(String code, String name, int byteOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%DB1.DBD%d:DINT", byteOffset), TagType.DINT, writable);
    }

    protected String registerRealVTag(String code, String name, int byteOffset, boolean writable) {
        return this.registerTag(code, name, String.format("%%DB1.DBD%d:REAL", byteOffset), TagType.REAL, writable);
    }

    protected void registerDevice(String code, String name, String type, String runningTagCode, String commandTagCode, String manualModeTagCode, String faultTagCode) {
        this.devices.put(code, new DeviceDef(code, name, type, runningTagCode, commandTagCode, manualModeTagCode, faultTagCode, null, null));
    }

    protected void registerDevice(String code, String name, String type, String runningTagCode, String commandTagCode, String manualModeTagCode, String faultTagCode, String openPositionTagCode, String closePositionTagCode) {
        this.devices.put(code, new DeviceDef(code, name, type, runningTagCode, commandTagCode, manualModeTagCode, faultTagCode, openPositionTagCode, closePositionTagCode));
    }

    protected void registerParameter(String code, String name, String tagCode) {
        this.parameters.put(code, new ParameterDef(code, name, tagCode));
    }

    protected Map<String, Object> buildPlcMeta() {
        LinkedHashMap<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("name", this.plcName);
        meta.put("series", this.series);
        meta.put("ip", this.plcIp);
        meta.put("rack", this.rack);
        meta.put("slot", this.slot);
        return meta;
    }

    protected List<Map<String, Object>> readDevices() {
        return this.readDevices(null);
    }

    protected List<Map<String, Object>> readDevices(Map<String, Object> snapshot) {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DeviceDef device : this.devices.values()) {
            Map<String, Object> values = snapshot == null ? this.readTags(device.getReadTags()) : snapshot;
            result.add(this.buildDeviceState(device, values));
        }
        return result;
    }

    protected List<Map<String, Object>> readParameters() {
        return this.readParameters(null);
    }

    protected List<Map<String, Object>> readParameters(Map<String, Object> snapshot) {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ParameterDef parameter : this.parameters.values()) {
            TagDef tag = this.tags.get(parameter.tagCode);
            Map<String, Object> values = snapshot == null ? this.readTags(Arrays.asList(parameter.tagCode)) : snapshot;
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", parameter.code);
            item.put("name", parameter.name);
            item.put("address", tag == null ? null : tag.address);
            item.put("type", tag == null ? null : tag.type.name());
            item.put("writable", tag != null && tag.writable);
            item.put("value", values.get(parameter.tagCode));
            result.add(item);
        }
        return result;
    }

    protected List<Map<String, Object>> catalogDevices() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (DeviceDef device : this.devices.values()) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", device.code);
            item.put("name", device.name);
            item.put("type", device.type);
            item.put("hasCommand", device.commandTagCode != null);
            item.put("hasManualMode", device.manualModeTagCode != null);
            item.put("tags", this.buildDeviceTagMeta(device));
            result.add(item);
        }
        return result;
    }

    protected List<Map<String, Object>> catalogParameters() {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ParameterDef parameter : this.parameters.values()) {
            TagDef tag = this.tags.get(parameter.tagCode);
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", parameter.code);
            item.put("name", parameter.name);
            item.put("address", tag == null ? null : tag.address);
            item.put("type", tag == null ? null : tag.type.name());
            item.put("writable", tag != null && tag.writable);
            result.add(item);
        }
        return result;
    }

    protected Map<String, Object> buildDeviceState(DeviceDef device, Map<String, Object> values) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("code", device.code);
        item.put("name", device.name);
        item.put("type", device.type);
        item.put("tags", this.buildDeviceTagMeta(device, values));
        this.putIfTagExists(item, "running", values, device.runningTagCode);
        this.putIfTagExists(item, "manualModeBit", values, device.manualModeTagCode);
        this.putIfTagExists(item, "fault", values, device.faultTagCode);
        this.putIfTagExists(item, "command", values, device.commandTagCode);
        this.putIfTagExists(item, "openPosition", values, device.openPositionTagCode);
        this.putIfTagExists(item, "closePosition", values, device.closePositionTagCode);
        return item;
    }

    protected List<Map<String, Object>> buildDeviceTagMeta(DeviceDef device) {
        return this.buildDeviceTagMeta(device, null);
    }

    protected List<Map<String, Object>> buildDeviceTagMeta(DeviceDef device, Map<String, Object> values) {
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        this.addDeviceTagMeta(result, "running", device.runningTagCode, values);
        this.addDeviceTagMeta(result, "manualModeBit", device.manualModeTagCode, values);
        this.addDeviceTagMeta(result, "fault", device.faultTagCode, values);
        this.addDeviceTagMeta(result, "command", device.commandTagCode, values);
        this.addDeviceTagMeta(result, "openPosition", device.openPositionTagCode, values);
        this.addDeviceTagMeta(result, "closePosition", device.closePositionTagCode, values);
        return result;
    }

    protected void addDeviceTagMeta(List<Map<String, Object>> result, String role, String tagCode) {
        this.addDeviceTagMeta(result, role, tagCode, null);
    }

    protected void addDeviceTagMeta(List<Map<String, Object>> result, String role, String tagCode, Map<String, Object> values) {
        if (tagCode == null) {
            return;
        }
        TagDef tag = this.tags.get(tagCode);
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("role", role);
        item.put("code", tagCode);
        item.put("name", tag == null ? tagCode : tag.name);
        item.put("address", tag == null ? null : tag.address);
        item.put("type", tag == null ? null : tag.type.name());
        item.put("writable", tag != null && tag.writable);
        if (values != null) {
            item.put("value", values.get(tagCode));
        }
        result.add(item);
    }

    protected void putIfTagExists(Map<String, Object> target, String key, Map<String, Object> values, String tagCode) {
        if (tagCode != null) {
            target.put(key, values.get(tagCode));
        }
    }

    protected DeviceDef requireDevice(String deviceCode) {
        DeviceDef device = this.devices.get(deviceCode);
        if (device == null) {
            throw new IllegalArgumentException("Unknown device: " + deviceCode);
        }
        return device;
    }

    protected String registerTag(String code, String name, String address, TagType type, boolean writable) {
        this.tags.put(code, new TagDef(code, name, address, type, writable));
        return code;
    }

    protected TagDef requireWritableTag(String tagCode) {
        TagDef tag = this.tags.get(tagCode);
        if (tag == null) {
            throw new IllegalArgumentException("Unknown tag: " + tagCode);
        }
        if (!tag.writable) {
            throw new IllegalArgumentException("Tag is read-only: " + tagCode);
        }
        return tag;
    }

    protected Map<String, Object> readTags(final Collection<String> tagCodes) {
        return this.withClient(new Snap7Callback<Map<String, Object>>(){

            @Override
            public Map<String, Object> execute(S7Client client) throws Exception {
                LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
                List requests = AbstractS7200SmartPlcService.this.buildReadRequests(tagCodes);
                for (ReadBatch batch : AbstractS7200SmartPlcService.this.buildReadBatches(requests)) {
                    byte[] buffer = new byte[batch.length];
                    AbstractS7200SmartPlcService.this.readArea(client, batch.area, batch.dbNumber, batch.start, buffer);
                    for (ReadRequest request : batch.requests) {
                        values.put(request.tag.code, AbstractS7200SmartPlcService.this.readTagValue(buffer, batch.start, request));
                    }
                }
                return values;
            }
        }, "readTags " + this.describeTags(tagCodes));
    }

    private String describeTags(Collection<String> tagCodes) {
        ArrayList<String> result = new ArrayList<String>();
        for (String tagCode : tagCodes) {
            TagDef tag = this.tags.get(tagCode);
            if (tag != null) {
                result.add(tag.code + "=" + tag.address);
                continue;
            }
            result.add(tagCode + "=<missing>");
        }
        return ((Object)result).toString();
    }

    protected boolean writeBoolTag(String tagCode, final boolean value) {
        final TagDef tag = this.requireWritableTag(tagCode);
        if (tag.type != TagType.BOOL) {
            throw new IllegalArgumentException("Tag type mismatch for " + tagCode);
        }
        boolean success = this.withClient(new Snap7Callback<Boolean>(){

            @Override
            public Boolean execute(S7Client client) throws Exception {
                AbstractS7200SmartPlcService.this.writeTagValue(client, tag, value);
                return Boolean.TRUE;
            }
        }, "writeBoolTag " + tagCode + "=" + value);
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    protected boolean writeIntTag(String tagCode, final int value) {
        final TagDef tag = this.requireWritableTag(tagCode);
        if (tag.type != TagType.INT) {
            throw new IllegalArgumentException("Tag type mismatch for " + tagCode);
        }
        boolean success = this.withClient(new Snap7Callback<Boolean>(){

            @Override
            public Boolean execute(S7Client client) throws Exception {
                AbstractS7200SmartPlcService.this.writeTagValue(client, tag, value);
                return Boolean.TRUE;
            }
        }, "writeIntTag " + tagCode + "=" + value);
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    protected boolean writeRealTag(String tagCode, final float value) {
        final TagDef tag = this.requireWritableTag(tagCode);
        if (tag.type != TagType.REAL) {
            throw new IllegalArgumentException("Tag type mismatch for " + tagCode);
        }
        boolean success = this.withClient(new Snap7Callback<Boolean>(){

            @Override
            public Boolean execute(S7Client client) throws Exception {
                AbstractS7200SmartPlcService.this.writeTagValue(client, tag, Float.valueOf(value));
                return Boolean.TRUE;
            }
        }, "writeRealTag " + tagCode + "=" + value);
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    protected boolean writeDIntTag(String tagCode, final int value) {
        final TagDef tag = this.requireWritableTag(tagCode);
        if (tag.type != TagType.DINT) {
            throw new IllegalArgumentException("Tag type mismatch for " + tagCode);
        }
        boolean success = this.withClient(new Snap7Callback<Boolean>(){

            @Override
            public Boolean execute(S7Client client) throws Exception {
                AbstractS7200SmartPlcService.this.writeTagValue(client, tag, value);
                return Boolean.TRUE;
            }
        }, "writeDIntTag " + tagCode + "=" + value);
        if (success) {
            this.invalidateOverviewCache();
        }
        return success;
    }

    protected boolean pulseBoolTag(String tagCode, long pulseMs) {
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

    private Object readTagValue(S7Client client, TagDef tag) throws Exception {
        switch (tag.type) {
            case BOOL: {
                return this.readBoolValue(client, tag.address);
            }
            case INT: {
                return this.readIntValue(client, tag.address);
            }
            case DINT: {
                return this.readDIntValue(client, tag.address);
            }
            case REAL: {
                return Float.valueOf(this.readRealValue(client, tag.address));
            }
        }
        throw new IllegalArgumentException("Unsupported tag type: " + (Object)((Object)tag.type));
    }

    private Object readTagValue(byte[] batchBuffer, int batchStart, ReadRequest request) {
        int relativeOffset = request.start - batchStart;
        switch (request.tag.type) {
            case BOOL: {
                return S7.GetBitAt((byte[])batchBuffer, (int)relativeOffset, (int)request.bitOffset);
            }
            case INT: {
                return S7.GetShortAt((byte[])batchBuffer, (int)relativeOffset);
            }
            case DINT: {
                return S7.GetDIntAt((byte[])batchBuffer, (int)relativeOffset);
            }
            case REAL: {
                return Float.valueOf(S7.GetFloatAt((byte[])batchBuffer, (int)relativeOffset));
            }
        }
        throw new IllegalArgumentException("Unsupported tag type: " + (Object)((Object)request.tag.type));
    }

    private void writeTagValue(S7Client client, TagDef tag, Object value) throws Exception {
        switch (tag.type) {
            case BOOL: {
                this.writeBoolValue(client, tag.address, (Boolean)value);
                return;
            }
            case INT: {
                this.writeIntValue(client, tag.address, (Integer)value);
                return;
            }
            case DINT: {
                this.writeDIntValue(client, tag.address, (Integer)value);
                return;
            }
            case REAL: {
                this.writeRealValue(client, tag.address, ((Float)value).floatValue());
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported tag type: " + (Object)((Object)tag.type));
    }

    private boolean readBoolValue(S7Client client, String address) throws Exception {
        Matcher matcher = BOOL_AREA_PATTERN.matcher(address);
        if (matcher.matches()) {
            int area = this.resolveArea(matcher.group(1).charAt(0));
            int byteOffset = Integer.parseInt(matcher.group(2));
            int bitOffset = Integer.parseInt(matcher.group(3));
            byte[] buffer = new byte[1];
            this.readArea(client, area, 0, byteOffset, buffer);
            return S7.GetBitAt((byte[])buffer, (int)0, (int)bitOffset);
        }
        matcher = BOOL_DBX_PATTERN.matcher(address);
        if (matcher.matches()) {
            int dbNumber = Integer.parseInt(matcher.group(1));
            int byteOffset = Integer.parseInt(matcher.group(2));
            int bitOffset = Integer.parseInt(matcher.group(3));
            byte[] buffer = new byte[1];
            this.readArea(client, 132, dbNumber, byteOffset, buffer);
            return S7.GetBitAt((byte[])buffer, (int)0, (int)bitOffset);
        }
        throw new IllegalArgumentException("Unsupported BOOL address: " + address);
    }

    private int readIntValue(S7Client client, String address) throws Exception {
        Matcher matcher = INT_AREA_WORD_PATTERN.matcher(address);
        if (matcher.matches()) {
            int area = this.resolveArea(matcher.group(1).charAt(0));
            int byteOffset = Integer.parseInt(matcher.group(2));
            byte[] buffer = new byte[2];
            this.readArea(client, area, 0, byteOffset, buffer);
            return S7.GetShortAt((byte[])buffer, (int)0);
        }
        matcher = INT_DBW_PATTERN.matcher(address);
        if (matcher.matches()) {
            int dbNumber = Integer.parseInt(matcher.group(1));
            int byteOffset = Integer.parseInt(matcher.group(2));
            byte[] buffer = new byte[2];
            this.readArea(client, 132, dbNumber, byteOffset, buffer);
            return S7.GetShortAt((byte[])buffer, (int)0);
        }
        throw new IllegalArgumentException("Unsupported INT address: " + address);
    }

    private int readDIntValue(S7Client client, String address) throws Exception {
        Matcher matcher = DINT_DBD_PATTERN.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported DINT address: " + address);
        }
        int dbNumber = Integer.parseInt(matcher.group(1));
        int byteOffset = Integer.parseInt(matcher.group(2));
        byte[] buffer = new byte[4];
        this.readArea(client, 132, dbNumber, byteOffset, buffer);
        return S7.GetDIntAt((byte[])buffer, (int)0);
    }

    private float readRealValue(S7Client client, String address) throws Exception {
        Matcher matcher = REAL_DBD_PATTERN.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported REAL address: " + address);
        }
        int dbNumber = Integer.parseInt(matcher.group(1));
        int byteOffset = Integer.parseInt(matcher.group(2));
        byte[] buffer = new byte[4];
        this.readArea(client, 132, dbNumber, byteOffset, buffer);
        return S7.GetFloatAt((byte[])buffer, (int)0);
    }

    private void writeBoolValue(S7Client client, String address, boolean value) throws Exception {
        Matcher matcher = BOOL_AREA_PATTERN.matcher(address);
        if (matcher.matches()) {
            int area = this.resolveArea(matcher.group(1).charAt(0));
            int byteOffset = Integer.parseInt(matcher.group(2));
            int bitOffset = Integer.parseInt(matcher.group(3));
            byte[] buffer = new byte[1];
            this.readArea(client, area, 0, byteOffset, buffer);
            S7.SetBitAt((byte[])buffer, (int)0, (int)bitOffset, (boolean)value);
            this.writeArea(client, area, 0, byteOffset, buffer);
            return;
        }
        matcher = BOOL_DBX_PATTERN.matcher(address);
        if (matcher.matches()) {
            int dbNumber = Integer.parseInt(matcher.group(1));
            int byteOffset = Integer.parseInt(matcher.group(2));
            int bitOffset = Integer.parseInt(matcher.group(3));
            byte[] buffer = new byte[1];
            this.readArea(client, 132, dbNumber, byteOffset, buffer);
            S7.SetBitAt((byte[])buffer, (int)0, (int)bitOffset, (boolean)value);
            this.writeArea(client, 132, dbNumber, byteOffset, buffer);
            return;
        }
        throw new IllegalArgumentException("Unsupported BOOL address: " + address);
    }

    private void writeIntValue(S7Client client, String address, int value) throws Exception {
        Matcher matcher = INT_AREA_WORD_PATTERN.matcher(address);
        if (matcher.matches()) {
            int area = this.resolveArea(matcher.group(1).charAt(0));
            int byteOffset = Integer.parseInt(matcher.group(2));
            byte[] buffer = new byte[2];
            S7.SetShortAt((byte[])buffer, (int)0, (int)value);
            this.writeArea(client, area, 0, byteOffset, buffer);
            return;
        }
        matcher = INT_DBW_PATTERN.matcher(address);
        if (matcher.matches()) {
            int dbNumber = Integer.parseInt(matcher.group(1));
            int byteOffset = Integer.parseInt(matcher.group(2));
            byte[] buffer = new byte[2];
            S7.SetShortAt((byte[])buffer, (int)0, (int)value);
            this.writeArea(client, 132, dbNumber, byteOffset, buffer);
            return;
        }
        throw new IllegalArgumentException("Unsupported INT address: " + address);
    }

    private void writeDIntValue(S7Client client, String address, int value) throws Exception {
        Matcher matcher = DINT_DBD_PATTERN.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported DINT address: " + address);
        }
        int dbNumber = Integer.parseInt(matcher.group(1));
        int byteOffset = Integer.parseInt(matcher.group(2));
        byte[] buffer = new byte[4];
        S7.SetDIntAt((byte[])buffer, (int)0, (int)value);
        this.writeArea(client, 132, dbNumber, byteOffset, buffer);
    }

    private void writeRealValue(S7Client client, String address, float value) throws Exception {
        Matcher matcher = REAL_DBD_PATTERN.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported REAL address: " + address);
        }
        int dbNumber = Integer.parseInt(matcher.group(1));
        int byteOffset = Integer.parseInt(matcher.group(2));
        byte[] buffer = new byte[4];
        S7.SetFloatAt((byte[])buffer, (int)0, (float)value);
        this.writeArea(client, 132, dbNumber, byteOffset, buffer);
    }

    private int resolveArea(char areaCode) {
        switch (areaCode) {
            case 'I': {
                return 129;
            }
            case 'Q': {
                return 130;
            }
            case 'M': {
                return 131;
            }
        }
        throw new IllegalArgumentException("Unsupported area code: " + areaCode);
    }

    private void readArea(S7Client client, int area, int dbNumber, int start, byte[] buffer) {
        int result = client.ReadArea(area, dbNumber, start, buffer.length, buffer);
        if (result != 0) {
            throw new IllegalStateException("Snap7 read failed, area=" + area + ", db=" + dbNumber + ", start=" + start + ", size=" + buffer.length + ", error=" + S7Client.ErrorText((int)result));
        }
    }

    private void writeArea(S7Client client, int area, int dbNumber, int start, byte[] buffer) {
        int result = client.WriteArea(area, dbNumber, start, buffer.length, buffer);
        if (result != 0) {
            throw new IllegalStateException("Snap7 write failed, area=" + area + ", db=" + dbNumber + ", start=" + start + ", size=" + buffer.length + ", error=" + S7Client.ErrorText((int)result));
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private <T> T withClient(Snap7Callback<T> callback, String action) {
        T t;
        S7Client client = null;
        boolean invalidate = false;
        try {
            client = (S7Client)this.snap7ClientPool.borrowObject();
            t = callback.execute(client);
            if (client == null) return t;
        }
        catch (Exception ex) {
            try {
                invalidate = true;
                log.error("Smart200 Snap7 call failed, plc={}, ip={}, rack={}, slot={}, action={}, reason={}", new Object[]{this.plcName, this.plcIp, this.rack, this.slot, action, ex.getMessage(), ex});
                if (!(ex instanceof RuntimeException)) throw new IllegalStateException(ex.getMessage(), ex);
                throw (RuntimeException)ex;
            }
            catch (Throwable throwable) {
                if (client == null) throw throwable;
                try {
                    if (invalidate) {
                        this.snap7ClientPool.invalidateObject((Object)client);
                        throw throwable;
                    }
                    this.snap7ClientPool.returnObject((Object)client);
                    throw throwable;
                }
                catch (Exception poolEx) {
                    log.warn("Snap7 client pool cleanup failed, plc={}, ip={}, rack={}, slot={}, reason={}", new Object[]{this.plcName, this.plcIp, this.rack, this.slot, poolEx.getMessage(), poolEx});
                }
                throw throwable;
            }
        }
        try {
            if (invalidate) {
                this.snap7ClientPool.invalidateObject((Object)client);
                return t;
            }
            this.snap7ClientPool.returnObject((Object)client);
            return t;
        }
        catch (Exception poolEx) {
            log.warn("Snap7 client pool cleanup failed, plc={}, ip={}, rack={}, slot={}, reason={}", new Object[]{this.plcName, this.plcIp, this.rack, this.slot, poolEx.getMessage(), poolEx});
        }
        return t;
    }

    private GenericObjectPool<S7Client> createClientPool() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(5);
        config.setMaxIdle(3);
        config.setMinIdle(1);
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(3000L);
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        config.setJmxEnabled(false);
        config.setMinEvictableIdleTimeMillis(180000L);
        config.setTimeBetweenEvictionRunsMillis(60000L);
        BasePooledObjectFactory<S7Client> factory = new BasePooledObjectFactory<S7Client>(){

            public S7Client create() throws Exception {
                return AbstractS7200SmartPlcService.this.createConnectedClient();
            }

            public PooledObject<S7Client> wrap(S7Client client) {
                return new DefaultPooledObject((Object)client);
            }

            public boolean validateObject(PooledObject<S7Client> pooledObject) {
                S7Client client = (S7Client)pooledObject.getObject();
                return client != null && client.Connected;
            }

            public void destroyObject(PooledObject<S7Client> pooledObject) {
                S7Client client = (S7Client)pooledObject.getObject();
                if (client != null) {
                    client.Disconnect();
                }
            }
        };
        return new GenericObjectPool((PooledObjectFactory)factory, config);
    }

    private S7Client createConnectedClient() throws Exception {
        Exception lastException = null;
        for (int[] candidate : SMART_TSAP_CANDIDATES) {
            try {
                return this.connectWithTimeout(candidate[0], candidate[1]);
            }
            catch (Exception ex) {
                lastException = ex;
                log.warn("Smart200 Snap7 TSAP candidate failed, plc={}, ip={}, localTsap=0x{}, remoteTsap=0x{}, reason={}", new Object[]{this.plcName, this.plcIp, Integer.toHexString(candidate[0]), Integer.toHexString(candidate[1]), ex.getMessage()});
            }
        }
        try {
            return this.connectWithRackSlotTimeout();
        }
        catch (Exception ex) {
            if (lastException != null) {
                ex.addSuppressed(lastException);
            }
            throw ex;
        }
    }

    private S7Client connectWithTimeout(int localTsap, int remoteTsap) throws Exception {
        Future<S7Client> future = connectExecutor.submit(() -> {
            S7Client client = new S7Client();
            client.RecvTimeout = 5000;
            client.SetConnectionType((short)1);
            client.SetConnectionParams(this.plcIp, 102, localTsap, remoteTsap);
            int connectResult = client.Connect();
            if (connectResult != 0) {
                throw new IllegalStateException("Snap7 connect failed, ip=" + this.plcIp + ", localTsap=0x" + Integer.toHexString(localTsap) + ", remoteTsap=0x" + Integer.toHexString(remoteTsap) + ", error=" + S7Client.ErrorText((int)connectResult));
            }
            return client;
        });
        return this.waitForConnection(future, "localTsap=0x" + Integer.toHexString(localTsap) + ", remoteTsap=0x" + Integer.toHexString(remoteTsap));
    }

    private S7Client connectWithRackSlotTimeout() throws Exception {
        Future<S7Client> future = connectExecutor.submit(() -> {
            S7Client client = new S7Client();
            client.RecvTimeout = 5000;
            client.SetConnectionType((short)1);
            int connectResult = client.ConnectTo(this.plcIp, this.rack, this.slot);
            if (connectResult != 0) {
                throw new IllegalStateException("Snap7 connect failed, ip=" + this.plcIp + ", rack=" + this.rack + ", slot=" + this.slot + ", error=" + S7Client.ErrorText((int)connectResult));
            }
            return client;
        });
        return this.waitForConnection(future, "rack=" + this.rack + ", slot=" + this.slot);
    }

    private S7Client waitForConnection(Future<S7Client> future, String target) throws Exception {
        try {
            return future.get(5000L, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalStateException("Snap7 connect timeout (5000ms), ip=" + this.plcIp + ", " + target);
        }
    }

    @PreDestroy
    public void closeSnap7ClientPool() {
        this.snap7ClientPool.close();
    }

    protected void invalidateOverviewCache() {
        this.overviewCache.invalidate();
    }

    protected Map<String, Object> enrichOverview(Map<String, Object> overview, OverviewCache.CacheResult cacheResult) {
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

    private List<ReadRequest> buildReadRequests(Collection<String> tagCodes) {
        ArrayList<ReadRequest> requests = new ArrayList<ReadRequest>();
        for (String tagCode : tagCodes) {
            TagDef tag = this.tags.get(tagCode);
            if (tag == null) continue;
            requests.add(this.parseReadRequest(tag));
        }
        return requests;
    }

    private List<ReadBatch> buildReadBatches(List<ReadRequest> requests) {
        Collections.sort(requests, new Comparator<ReadRequest>(){

            @Override
            public int compare(ReadRequest left, ReadRequest right) {
                if (left.area != right.area) {
                    return left.area - right.area;
                }
                if (left.dbNumber != right.dbNumber) {
                    return left.dbNumber - right.dbNumber;
                }
                return left.start - right.start;
            }
        });
        ArrayList<ReadBatch> batches = new ArrayList<ReadBatch>();
        ReadBatch current = null;
        for (ReadRequest request : requests) {
            if (current == null || !current.canMerge(request)) {
                current = new ReadBatch(request);
                batches.add(current);
                continue;
            }
            current.merge(request);
        }
        return batches;
    }

    private ReadRequest parseReadRequest(TagDef tag) {
        Matcher matcher = BOOL_AREA_PATTERN.matcher(tag.address);
        if (matcher.matches()) {
            return new ReadRequest(tag, this.resolveArea(matcher.group(1).charAt(0)), 0, Integer.parseInt(matcher.group(2)), 1, Integer.parseInt(matcher.group(3)));
        }
        matcher = BOOL_DBX_PATTERN.matcher(tag.address);
        if (matcher.matches()) {
            return new ReadRequest(tag, 132, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), 1, Integer.parseInt(matcher.group(3)));
        }
        matcher = INT_AREA_WORD_PATTERN.matcher(tag.address);
        if (matcher.matches()) {
            return new ReadRequest(tag, this.resolveArea(matcher.group(1).charAt(0)), 0, Integer.parseInt(matcher.group(2)), 2, -1);
        }
        matcher = INT_DBW_PATTERN.matcher(tag.address);
        if (matcher.matches()) {
            return new ReadRequest(tag, 132, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), 2, -1);
        }
        matcher = DINT_DBD_PATTERN.matcher(tag.address);
        if (matcher.matches()) {
            return new ReadRequest(tag, 132, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), 4, -1);
        }
        matcher = REAL_DBD_PATTERN.matcher(tag.address);
        if (matcher.matches()) {
            return new ReadRequest(tag, 132, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), 4, -1);
        }
        throw new IllegalArgumentException("Unsupported tag address: " + tag.address);
    }

    protected static void addIfPresent(List<String> list, String value) {
        if (value != null) {
            list.add(value);
        }
    }

    protected static class TagDef {
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

    private static class ReadRequest {
        private final TagDef tag;
        private final int area;
        private final int dbNumber;
        private final int start;
        private final int size;
        private final int bitOffset;

        private ReadRequest(TagDef tag, int area, int dbNumber, int start, int size, int bitOffset) {
            this.tag = tag;
            this.area = area;
            this.dbNumber = dbNumber;
            this.start = start;
            this.size = size;
            this.bitOffset = bitOffset;
        }

        private int endExclusive() {
            return this.start + this.size;
        }
    }

    protected static class DeviceDef {
        private final String code;
        private final String name;
        private final String type;
        private final String runningTagCode;
        private final String commandTagCode;
        private final String manualModeTagCode;
        private final String faultTagCode;
        private final String openPositionTagCode;
        private final String closePositionTagCode;

        private DeviceDef(String code, String name, String type, String runningTagCode, String commandTagCode, String manualModeTagCode, String faultTagCode, String openPositionTagCode, String closePositionTagCode) {
            this.code = code;
            this.name = name;
            this.type = type;
            this.runningTagCode = runningTagCode;
            this.commandTagCode = commandTagCode;
            this.manualModeTagCode = manualModeTagCode;
            this.faultTagCode = faultTagCode;
            this.openPositionTagCode = openPositionTagCode;
            this.closePositionTagCode = closePositionTagCode;
        }

        private List<String> getReadTags() {
            ArrayList<String> result = new ArrayList<String>();
            AbstractS7200SmartPlcService.addIfPresent(result, this.runningTagCode);
            AbstractS7200SmartPlcService.addIfPresent(result, this.manualModeTagCode);
            AbstractS7200SmartPlcService.addIfPresent(result, this.faultTagCode);
            AbstractS7200SmartPlcService.addIfPresent(result, this.commandTagCode);
            AbstractS7200SmartPlcService.addIfPresent(result, this.openPositionTagCode);
            AbstractS7200SmartPlcService.addIfPresent(result, this.closePositionTagCode);
            return result;
        }
    }

    protected static class ParameterDef {
        private final String code;
        private final String name;
        private final String tagCode;

        private ParameterDef(String code, String name, String tagCode) {
            this.code = code;
            this.name = name;
            this.tagCode = tagCode;
        }
    }

    protected static enum TagType {
        BOOL,
        INT,
        DINT,
        REAL;

    }

    private static interface Snap7Callback<T> {
        public T execute(S7Client var1) throws Exception;
    }

    private static class ReadBatch {
        private final int area;
        private final int dbNumber;
        private int start;
        private int endExclusive;
        private int length;
        private final List<ReadRequest> requests = new ArrayList<ReadRequest>();

        private ReadBatch(ReadRequest request) {
            this.area = request.area;
            this.dbNumber = request.dbNumber;
            this.start = request.start;
            this.endExclusive = request.endExclusive();
            this.length = this.endExclusive - this.start;
            this.requests.add(request);
        }

        private boolean canMerge(ReadRequest request) {
            if (this.area != request.area || this.dbNumber != request.dbNumber) {
                return false;
            }
            if (request.start - this.endExclusive > 8) {
                return false;
            }
            int mergedEndExclusive = Math.max(this.endExclusive, request.endExclusive());
            return mergedEndExclusive - this.start <= 64;
        }

        private void merge(ReadRequest request) {
            if (request.start < this.start) {
                this.start = request.start;
            }
            if (request.endExclusive() > this.endExclusive) {
                this.endExclusive = request.endExclusive();
            }
            this.length = this.endExclusive - this.start;
            this.requests.add(request);
        }
    }
}

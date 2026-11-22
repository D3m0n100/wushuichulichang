/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.MonitorJdbcRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
public class MonitorPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(MonitorPersistenceService.class);
    private static final Map<String, String> POINT_CODE_ALIASES = MonitorPersistenceService.buildPointCodeAliases();
    private static final String PHASE1_FLOW = "Z-FIT01";
    private static final String PHASE2_FLOW = "Z-FIT02";
    private static final String PHASE1_TOTALIZER = "B1_PHASE1_FLOWMETER_TOTALIZER";
    private static final String PHASE2_TOTALIZER = "B1_PHASE2_FLOWMETER_TOTALIZER";
    private static final Duration MAX_VALUE_AGE = Duration.ofMinutes(2L);
    private static final ZoneId MONITOR_ZONE = ZoneId.of("Asia/Shanghai");
    private final MonitorJdbcRepository repository;
    private final Clock clock;
    private final Map<String, MonitorJdbcRepository.MonitorPointConfig> pointCache = new ConcurrentHashMap<String, MonitorJdbcRepository.MonitorPointConfig>();
    private volatile Map<String, CachedPointValue> latestPointValues = Collections.emptyMap();
    private volatile FlowSnapshot latestFlowSnapshot;

    @Autowired
    public MonitorPersistenceService(MonitorJdbcRepository monitorJdbcRepository) {
        this(monitorJdbcRepository, Clock.system(MONITOR_ZONE));
    }

    public MonitorPersistenceService(MonitorJdbcRepository monitorJdbcRepository, Clock clock) {
        this.repository = monitorJdbcRepository;
        this.clock = clock;
        this.refreshPointCache();
    }

    public void refreshPointCache() {
        List<MonitorJdbcRepository.MonitorPointConfig> list = this.repository.listEnabledPoints();
        LinkedHashMap<String, MonitorJdbcRepository.MonitorPointConfig> linkedHashMap = new LinkedHashMap<String, MonitorJdbcRepository.MonitorPointConfig>();
        for (MonitorJdbcRepository.MonitorPointConfig monitorPointConfig : list) {
            linkedHashMap.put(monitorPointConfig.getPointCode(), monitorPointConfig);
        }
        this.pointCache.clear();
        this.pointCache.putAll(linkedHashMap);
    }

    public void persistOverview(String string, Map<String, Object> map) {
        if (map == null) {
            return;
        }
        LocalDateTime localDateTime = LocalDateTime.now(this.clock);
        LinkedHashMap<String, BigDecimal> linkedHashMap = new LinkedHashMap<String, BigDecimal>();
        this.collectOverviewItems(map.get("instruments"), "output", linkedHashMap);
        this.collectOverviewItems(map.get("parameters"), "value", linkedHashMap);
        this.publishPlcValues(string, linkedHashMap, localDateTime);
        this.persistDeviceFaults(string, map.get("devices"), localDateTime);
    }

    private void collectOverviewItems(Object object, String string, Map<String, BigDecimal> map) {
        if (!(object instanceof List)) {
            return;
        }
        List list = (List)object;
        for (Object e : list) {
            String string2;
            BigDecimal bigDecimal;
            if (!(e instanceof Map)) continue;
            Map map2 = (Map)e;
            Object v = map2.get("code");
            Object v2 = map2.get(string);
            if (v == null || v2 == null || (bigDecimal = this.toDecimal(v2)) == null || !this.pointCache.containsKey(string2 = this.normalizePointCode(String.valueOf(v))) && !this.isTotalizerPoint(string2)) continue;
            map.put(string2, bigDecimal);
        }
    }

    private synchronized void publishPlcValues(String string, Map<String, BigDecimal> map, LocalDateTime localDateTime) {
        LinkedHashMap<String, CachedPointValue> linkedHashMap = new LinkedHashMap<String, CachedPointValue>(this.latestPointValues);
        linkedHashMap.entrySet().removeIf(entry -> string.equals(((CachedPointValue)entry.getValue()).plcCode));
        for (Map.Entry<String, BigDecimal> object : map.entrySet()) {
            linkedHashMap.put(object.getKey(), new CachedPointValue(object.getValue(), localDateTime, string));
        }
        this.latestPointValues = Collections.unmodifiableMap(linkedHashMap);
        if ("b1".equals(string)) {
            BigDecimal bigDecimal = map.get(PHASE1_FLOW);
            BigDecimal bigDecimal2 = map.get(PHASE2_FLOW);
            this.latestFlowSnapshot = bigDecimal == null || bigDecimal2 == null ? null : new FlowSnapshot(bigDecimal, bigDecimal2, localDateTime);
        }
    }

    public synchronized void invalidatePlcValues(String string) {
        LinkedHashMap<String, CachedPointValue> linkedHashMap = new LinkedHashMap<String, CachedPointValue>(this.latestPointValues);
        linkedHashMap.entrySet().removeIf(entry -> string.equals(((CachedPointValue)entry.getValue()).plcCode));
        this.latestPointValues = Collections.unmodifiableMap(linkedHashMap);
        if ("b1".equals(string)) {
            this.latestFlowSnapshot = null;
        }
    }

    @Transactional(transactionManager="monitorTransactionManager")
    public synchronized void persistMinute(LocalDateTime localDateTime) {
        LocalDateTime localDateTime2 = localDateTime.truncatedTo(ChronoUnit.MINUTES);
        FlowSnapshot flowSnapshot = this.freshFlowSnapshot();
        if (flowSnapshot == null) {
            this.persistMinuteValue(PHASE1_FLOW, this.freshValue(PHASE1_FLOW), localDateTime2);
            this.persistMinuteValue(PHASE2_FLOW, this.freshValue(PHASE2_FLOW), localDateTime2);
        } else {
            this.persistMinuteValue(PHASE1_FLOW, flowSnapshot.phase1, localDateTime2);
            this.persistMinuteValue(PHASE2_FLOW, flowSnapshot.phase2, localDateTime2);
        }
        CachedPointValue cachedPointValue = this.freshValue(PHASE1_TOTALIZER);
        CachedPointValue cachedPointValue2 = this.freshValue(PHASE2_TOTALIZER);
        this.repository.upsertTotalizerMinute(localDateTime2, cachedPointValue == null ? null : cachedPointValue.value, cachedPointValue2 == null ? null : cachedPointValue2.value);
    }

    public void persistHourly(LocalDateTime localDateTime) {
        LocalDateTime localDateTime2 = localDateTime.truncatedTo(ChronoUnit.HOURS);
        Map<String, CachedPointValue> map = this.latestPointValues;
        for (MonitorJdbcRepository.MonitorPointConfig monitorPointConfig : this.pointCache.values()) {
            CachedPointValue cachedPointValue;
            if (this.isFlowPoint(monitorPointConfig) || !"hour".equalsIgnoreCase(monitorPointConfig.getRawSaveCycleType()) || (cachedPointValue = this.freshValue(monitorPointConfig.getPointCode(), map)) == null) continue;
            this.repository.upsertHourlySnapshot(monitorPointConfig.getId(), monitorPointConfig.getPointCode(), localDateTime2, cachedPointValue.value);
        }
        this.repository.aggregateFlowHour(localDateTime2.minusHours(1L), localDateTime2);
    }

    public void persistDaily(LocalDate localDate) {
        Map<String, CachedPointValue> map = this.latestPointValues;
        for (MonitorJdbcRepository.MonitorPointConfig monitorPointConfig : this.pointCache.values()) {
            CachedPointValue cachedPointValue;
            if (this.isFlowPoint(monitorPointConfig) || !"hour".equalsIgnoreCase(monitorPointConfig.getRawSaveCycleType()) || (cachedPointValue = this.freshValue(monitorPointConfig.getPointCode(), map)) == null) continue;
            this.repository.upsertDailySnapshot(monitorPointConfig.getId(), monitorPointConfig.getPointCode(), localDate, cachedPointValue.value);
        }
        this.repository.aggregateFlowDay(localDate.minusDays(1L), localDate);
    }

    private boolean persistMinuteValue(String string, CachedPointValue cachedPointValue, LocalDateTime localDateTime) {
        return cachedPointValue != null && this.persistMinuteValue(string, cachedPointValue.value, localDateTime);
    }

    private boolean persistMinuteValue(String string, BigDecimal bigDecimal, LocalDateTime localDateTime) {
        MonitorJdbcRepository.MonitorPointConfig monitorPointConfig = this.pointCache.get(string);
        if (monitorPointConfig != null && bigDecimal != null) {
            return this.repository.insertPointDataIfAbsent(monitorPointConfig, localDateTime, bigDecimal);
        }
        return false;
    }

    private CachedPointValue freshValue(String string) {
        return this.freshValue(string, this.latestPointValues);
    }

    private CachedPointValue freshValue(String string, Map<String, CachedPointValue> map) {
        CachedPointValue cachedPointValue = map.get(string);
        if (cachedPointValue == null) {
            return null;
        }
        Duration duration = Duration.between(cachedPointValue.observedAt, LocalDateTime.now(this.clock));
        if (duration.isNegative() || duration.compareTo(MAX_VALUE_AGE) > 0) {
            return null;
        }
        return cachedPointValue;
    }

    private FlowSnapshot freshFlowSnapshot() {
        FlowSnapshot flowSnapshot = this.latestFlowSnapshot;
        if (flowSnapshot == null) {
            return null;
        }
        Duration duration = Duration.between(flowSnapshot.observedAt, LocalDateTime.now(this.clock));
        if (duration.isNegative() || duration.compareTo(MAX_VALUE_AGE) > 0) {
            return null;
        }
        return flowSnapshot;
    }

    private boolean isFlowPoint(MonitorJdbcRepository.MonitorPointConfig monitorPointConfig) {
        return PHASE1_FLOW.equals(monitorPointConfig.getPointCode()) || PHASE2_FLOW.equals(monitorPointConfig.getPointCode());
    }

    private boolean isTotalizerPoint(String string) {
        return PHASE1_TOTALIZER.equals(string) || PHASE2_TOTALIZER.equals(string);
    }

    private void persistDeviceFaults(String string, Object object, LocalDateTime localDateTime) {
        if (!(object instanceof List)) {
            return;
        }
        List list = (List)object;
        for (Object e : list) {
            Boolean bl;
            if (!(e instanceof Map)) continue;
            Map map = (Map)e;
            Object v = map.get("code");
            Object v2 = map.get("fault");
            if (v == null || v2 == null || (bl = this.toBoolean(v2)) == null) continue;
            String string2 = String.valueOf(v);
            String string3 = map.get("name") == null ? string2 : String.valueOf(map.get("name"));
            String string4 = map.get("type") == null ? null : String.valueOf(map.get("type"));
            this.persistDeviceFault(string, string2, string3, string4, bl, localDateTime);
        }
    }

    private void persistDeviceFault(String string, String string2, String string3, String string4, boolean bl, LocalDateTime localDateTime) {
        try {
            MonitorJdbcRepository.DeviceFaultState deviceFaultState = this.repository.findDeviceFaultState(string, string2);
            LocalDateTime localDateTime2 = deviceFaultState == null || deviceFaultState.isFaultStatus() != bl ? localDateTime : deviceFaultState.getLastChangeTime();
            LocalDateTime localDateTime3 = localDateTime2;
            if (localDateTime2 == null) {
                localDateTime2 = localDateTime;
            }
            if (deviceFaultState == null && bl || deviceFaultState != null && deviceFaultState.isFaultStatus() != bl) {
                this.repository.insertDeviceFaultEvent(string, string2, string3, string4, bl, bl ? "ACTIVE" : "RECOVERED", localDateTime);
            }
            this.repository.upsertDeviceFaultState(string, string2, string3, string4, bl, localDateTime2, localDateTime);
        }
        catch (Exception exception) {
            log.warn("Persist device fault failed. plc={}, deviceCode={}", new Object[]{string, string2, exception});
        }
    }

    private BigDecimal toDecimal(Object object) {
        if (object instanceof BigDecimal) {
            return ((BigDecimal)object).setScale(3, RoundingMode.HALF_UP);
        }
        if (object instanceof Number) {
            double d = ((Number)object).doubleValue();
            if (!Double.isFinite(d)) {
                return null;
            }
            return BigDecimal.valueOf(d).setScale(3, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(String.valueOf(object)).setScale(3, RoundingMode.HALF_UP);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private Boolean toBoolean(Object object) {
        if (object instanceof Boolean) {
            return (Boolean)object;
        }
        if (object instanceof Number) {
            return ((Number)object).intValue() != 0;
        }
        String string = String.valueOf(object).trim();
        if ("1".equals(string) || "true".equalsIgnoreCase(string) || "yes".equalsIgnoreCase(string)) {
            return Boolean.TRUE;
        }
        if ("0".equals(string) || "false".equalsIgnoreCase(string) || "no".equalsIgnoreCase(string)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private String normalizePointCode(String string) {
        if (string == null) {
            return null;
        }
        String string2 = string.trim();
        String string3 = POINT_CODE_ALIASES.get(string2);
        if (string3 != null) {
            return string3;
        }
        return string2.replace('_', '-');
    }

    private static Map<String, String> buildPointCodeAliases() {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        linkedHashMap.put("B1_PHASE1_FLOWMETER", PHASE1_FLOW);
        linkedHashMap.put("B1_PHASE2_FLOWMETER", PHASE2_FLOW);
        linkedHashMap.put(PHASE1_TOTALIZER, PHASE1_TOTALIZER);
        linkedHashMap.put(PHASE2_TOTALIZER, PHASE2_TOTALIZER);
        linkedHashMap.put("B1_INFLOW_TURBIDITY", "Z-AIT01");
        linkedHashMap.put("B1_INFLOW_PH", "Z-AIT02");
        linkedHashMap.put("B1_INFLOW_NH3N", "Z-AIT03");
        linkedHashMap.put("B1_INFLOW_COD", "Z-AIT04");
        linkedHashMap.put("B1_INFLOW_TN", "Z-AIT05");
        linkedHashMap.put("B1_INFLOW_TP", "Z-AIT06");
        return linkedHashMap;
    }

    private static final class CachedPointValue {
        private final BigDecimal value;
        private final LocalDateTime observedAt;
        private final String plcCode;

        private CachedPointValue(BigDecimal bigDecimal, LocalDateTime localDateTime, String string) {
            this.value = bigDecimal;
            this.observedAt = localDateTime;
            this.plcCode = string;
        }
    }

    private static final class FlowSnapshot {
        private final BigDecimal phase1;
        private final BigDecimal phase2;
        private final LocalDateTime observedAt;

        private FlowSnapshot(BigDecimal bigDecimal, BigDecimal bigDecimal2, LocalDateTime localDateTime) {
            this.phase1 = bigDecimal;
            this.phase2 = bigDecimal2;
            this.observedAt = localDateTime;
        }
    }
}

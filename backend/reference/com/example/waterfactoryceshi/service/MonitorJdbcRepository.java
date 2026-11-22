/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.stereotype.Repository
 */
package com.example.waterfactoryceshi.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
public class MonitorJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public MonitorJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MonitorPointConfig> listEnabledPoints() {
        return this.jdbcTemplate.query("SELECT id, point_code, point_name, area_name, category_name, object_name, instrument_name, unit, raw_save_cycle_type, raw_save_cycle_value, need_hourly_avg, need_daily_avg, need_totalizer FROM monitor_point WHERE enabled = 1", (resultSet, n) -> this.mapPointConfig(resultSet));
    }

    public MonitorPointConfig findByCode(String string) {
        List list = this.jdbcTemplate.query("SELECT id, point_code, point_name, area_name, category_name, object_name, instrument_name, unit, raw_save_cycle_type, raw_save_cycle_value, need_hourly_avg, need_daily_avg, need_totalizer FROM monitor_point WHERE point_code = ?", new Object[]{string}, (resultSet, n) -> this.mapPointConfig(resultSet));
        return list.isEmpty() ? null : (MonitorPointConfig)list.get(0);
    }

    public MonitorPointConfig ensurePointExists(String string, String string2) {
        MonitorPointConfig monitorPointConfig = this.findByCode(string);
        if (monitorPointConfig != null) {
            return monitorPointConfig;
        }
        this.jdbcTemplate.update("INSERT INTO monitor_point (seq_no, point_code, point_name, area_name, category_name, object_name, instrument_name, unit, value_type, raw_save_cycle_type, raw_save_cycle_value, need_hourly_avg, need_daily_avg, need_totalizer, aggregate_group, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DECIMAL', 'minute', 1, 1, 1, 0, NULL, 1)", new Object[]{9999, string, string2, "plc-auto-discovery", "realtime", string2, string2, null});
        return this.findByCode(string);
    }

    public void insertPointData(MonitorPointConfig monitorPointConfig, LocalDateTime localDateTime, BigDecimal bigDecimal) {
        this.jdbcTemplate.update("INSERT INTO monitor_point_data (point_id, point_code, collect_time, value_decimal, value_text, quality_status, source_type) VALUES (?, ?, ?, ?, NULL, 'GOOD', 'PLC')", new Object[]{monitorPointConfig.getId(), monitorPointConfig.getPointCode(), Timestamp.valueOf(localDateTime), bigDecimal});
    }

    public void insertTotalizerSnapshot(MonitorPointConfig monitorPointConfig, LocalDateTime localDateTime, BigDecimal bigDecimal) {
        this.jdbcTemplate.update("INSERT INTO monitor_totalizer_snapshot (collect_time, total_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE total_value = VALUES(total_value)", new Object[]{Timestamp.valueOf(localDateTime), bigDecimal});
    }

    public boolean insertPointDataIfAbsent(MonitorPointConfig monitorPointConfig, LocalDateTime localDateTime, BigDecimal bigDecimal) {
        int n = this.jdbcTemplate.update("INSERT IGNORE INTO monitor_point_data (point_id, point_code, collect_time, value_decimal, value_text, quality_status, source_type) VALUES (?, ?, ?, ?, NULL, 'GOOD', 'PLC')", new Object[]{monitorPointConfig.getId(), monitorPointConfig.getPointCode(), Timestamp.valueOf(localDateTime), bigDecimal});
        return n > 0;
    }

    public Map<String, BigDecimal> findFlowMinuteValues(LocalDateTime localDateTime) {
        List list = this.jdbcTemplate.queryForList("SELECT point_code, value_decimal FROM monitor_point_data WHERE point_code IN ('Z-FIT01', 'Z-FIT02') AND collect_time = ?", new Object[]{Timestamp.valueOf(localDateTime)});
        LinkedHashMap<String, BigDecimal> linkedHashMap = new LinkedHashMap<String, BigDecimal>();
        for (Map map : list) {
            Object v = map.get("point_code");
            Object v2 = map.get("value_decimal");
            if (v == null || !(v2 instanceof BigDecimal)) continue;
            linkedHashMap.put(String.valueOf(v), (BigDecimal)v2);
        }
        return linkedHashMap;
    }

    public void upsertHourlySnapshot(long l, String string, LocalDateTime localDateTime, BigDecimal bigDecimal) {
        this.jdbcTemplate.update("INSERT INTO monitor_point_hourly (point_id, point_code, stat_hour, avg_value, min_value, max_value, sample_count) VALUES (?, ?, ?, ?, ?, ?, 1) ON DUPLICATE KEY UPDATE avg_value = VALUES(avg_value), min_value = VALUES(min_value), max_value = VALUES(max_value), sample_count = 1", new Object[]{l, string, Timestamp.valueOf(localDateTime), bigDecimal, bigDecimal, bigDecimal});
    }

    public void upsertDailySnapshot(long l, String string, LocalDate localDate, BigDecimal bigDecimal) {
    }

    public void aggregateFlowHour(LocalDateTime localDateTime, LocalDateTime localDateTime2) {
        this.jdbcTemplate.update("INSERT INTO monitor_metric_hourly (metric_code, metric_name, stat_hour, value_decimal, sample_count) SELECT d.point_code, CASE d.point_code WHEN 'Z-FIT01' THEN '\u4e00\u671f\u8fdb\u6c34\u5c0f\u65f6\u5e73\u5747\u6d41\u91cf' ELSE '\u4e8c\u671f\u8fdb\u6c34\u5c0f\u65f6\u5e73\u5747\u6d41\u91cf' END, ?, ROUND(AVG(d.value_decimal), 3), COUNT(*) FROM monitor_point_data d WHERE d.point_code IN ('Z-FIT01', 'Z-FIT02') AND d.value_decimal IS NOT NULL AND d.collect_time >= ? AND d.collect_time < ? GROUP BY d.point_code ON DUPLICATE KEY UPDATE metric_name = VALUES(metric_name), value_decimal = VALUES(value_decimal), sample_count = VALUES(sample_count)", new Object[]{Timestamp.valueOf(localDateTime), Timestamp.valueOf(localDateTime), Timestamp.valueOf(localDateTime2)});
    }

    public void aggregateFlowDay(LocalDate localDate, LocalDate localDate2) {
        this.jdbcTemplate.update("INSERT INTO monitor_point_daily (point_id, point_code, stat_date, avg_value, min_value, max_value, sample_count) SELECT h.point_id, h.point_code, ?, ROUND(AVG(h.avg_value), 3), MIN(h.min_value), MAX(h.max_value), SUM(h.sample_count) FROM monitor_point_hourly h WHERE h.point_code NOT IN ('Z-FIT01', 'Z-FIT02') AND h.avg_value IS NOT NULL AND h.stat_hour >= ? AND h.stat_hour < ? GROUP BY h.point_id, h.point_code ON DUPLICATE KEY UPDATE avg_value = VALUES(avg_value), min_value = VALUES(min_value), max_value = VALUES(max_value), sample_count = VALUES(sample_count)", new Object[]{localDate, localDate, localDate2});
        this.jdbcTemplate.update("INSERT INTO monitor_metric_daily (metric_code, metric_name, stat_date, value_decimal, sample_count) SELECT d.point_code, CASE d.point_code WHEN 'Z-FIT01' THEN '\u4e00\u671f\u8fdb\u6c34\u65e5\u5e73\u5747\u6d41\u91cf' ELSE '\u4e8c\u671f\u8fdb\u6c34\u65e5\u5e73\u5747\u6d41\u91cf' END, ?, ROUND(AVG(d.value_decimal), 3), COUNT(*) FROM monitor_point_data d WHERE d.point_code IN ('Z-FIT01', 'Z-FIT02') AND d.value_decimal IS NOT NULL AND d.collect_time >= ? AND d.collect_time < ? GROUP BY d.point_code ON DUPLICATE KEY UPDATE metric_name = VALUES(metric_name), value_decimal = VALUES(value_decimal), sample_count = VALUES(sample_count)", new Object[]{localDate, localDate, localDate2});
    }

    public void upsertTotalizerMinute(LocalDateTime localDateTime, BigDecimal bigDecimal, BigDecimal bigDecimal2) {
        Timestamp timestamp = Timestamp.valueOf(localDateTime);
        if (bigDecimal != null) {
            this.jdbcTemplate.update("INSERT INTO monitor_phase1_totalizer (collect_time, total_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE total_value = VALUES(total_value)", new Object[]{timestamp, bigDecimal});
        }
        if (bigDecimal2 != null) {
            this.jdbcTemplate.update("INSERT INTO monitor_phase2_totalizer (collect_time, total_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE total_value = VALUES(total_value)", new Object[]{timestamp, bigDecimal2});
        }
        this.jdbcTemplate.update("INSERT INTO monitor_totalizer_snapshot (collect_time, total_value) SELECT p1.collect_time, ROUND(p1.total_value + p2.total_value, 3) FROM monitor_phase1_totalizer p1 JOIN monitor_phase2_totalizer p2 ON p2.collect_time = p1.collect_time WHERE p1.collect_time = ? ON DUPLICATE KEY UPDATE total_value = VALUES(total_value)", new Object[]{timestamp});
    }

    public boolean appendCombinedTotalizer(LocalDateTime localDateTime, BigDecimal bigDecimal) {
        int n = this.jdbcTemplate.update("INSERT INTO monitor_totalizer_snapshot (collect_time, total_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE total_value = VALUES(total_value)", new Object[]{Timestamp.valueOf(localDateTime), bigDecimal});
        return n > 0;
    }

    private void refreshInflowHourlyMetric(LocalDateTime localDateTime) {
    }

    private void refreshInflowDailyMetric(LocalDate localDate) {
    }

    public DeviceFaultState findDeviceFaultState(String string, String string2) {
        List list = this.jdbcTemplate.query("SELECT fault_status, last_change_time FROM monitor_device_fault_state WHERE plc_code = ? AND device_code = ?", new Object[]{string, string2}, (resultSet, n) -> new DeviceFaultState(resultSet.getInt("fault_status") == 1, this.toLocalDateTime(resultSet.getTimestamp("last_change_time"))));
        return list.isEmpty() ? null : (DeviceFaultState)list.get(0);
    }

    public void upsertDeviceFaultState(String string, String string2, String string3, String string4, boolean bl, LocalDateTime localDateTime, LocalDateTime localDateTime2) {
        this.jdbcTemplate.update("INSERT INTO monitor_device_fault_state (plc_code, device_code, device_name, device_type, fault_status, last_change_time, last_seen_time) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE device_name = VALUES(device_name), device_type = VALUES(device_type), fault_status = VALUES(fault_status), last_change_time = VALUES(last_change_time), last_seen_time = VALUES(last_seen_time)", new Object[]{string, string2, string3, string4, bl ? 1 : 0, Timestamp.valueOf(localDateTime), Timestamp.valueOf(localDateTime2)});
    }

    public void insertDeviceFaultEvent(String string, String string2, String string3, String string4, boolean bl, String string5, LocalDateTime localDateTime) {
        this.jdbcTemplate.update("INSERT INTO monitor_device_fault_event (plc_code, device_code, device_name, device_type, fault_status, event_type, event_time) VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[]{string, string2, string3, string4, bl ? 1 : 0, string5, Timestamp.valueOf(localDateTime)});
    }

    public List<Map<String, Object>> listPoints() {
        return this.jdbcTemplate.queryForList("SELECT point_code, point_name, area_name, category_name, object_name, instrument_name, unit, raw_save_cycle_type, raw_save_cycle_value, need_hourly_avg, need_daily_avg, need_totalizer, enabled FROM monitor_point WHERE enabled = 1 ORDER BY seq_no, id");
    }

    public List<Map<String, Object>> listLatestPointData(String string, int n) {
        int n2 = this.normalizeLimit(n, 1, 1000, 100);
        return this.jdbcTemplate.queryForList("SELECT point_code, collect_time, value_decimal, value_text, quality_status, source_type FROM monitor_point_data WHERE point_code = ? ORDER BY collect_time DESC LIMIT ?", new Object[]{string, n2});
    }

    public List<Map<String, Object>> listPointHourly(String string, int n) {
        int n2 = this.normalizeLimit(n, 1, 1000, 24);
        return this.jdbcTemplate.queryForList("SELECT point_code, stat_hour, avg_value, min_value, max_value, sample_count FROM monitor_point_hourly WHERE point_code = ? ORDER BY stat_hour DESC LIMIT ?", new Object[]{string, n2});
    }

    public List<Map<String, Object>> listPointDaily(String string, int n) {
        int n2 = this.normalizeLimit(n, 1, 1000, 31);
        return this.jdbcTemplate.queryForList("SELECT point_code, stat_date, avg_value, min_value, max_value, sample_count FROM monitor_point_daily WHERE point_code = ? ORDER BY stat_date DESC LIMIT ?", new Object[]{string, n2});
    }

    public List<Map<String, Object>> listMetricHourly(String string, int n) {
        int n2 = this.normalizeLimit(n, 1, 1000, 24);
        if ("INFLOW_TOTAL_HOURLY_AVG".equals(string)) {
            return this.jdbcTemplate.queryForList("SELECT metric_code, metric_name, stat_hour, value_decimal, sample_count FROM monitor_metric_hourly WHERE metric_code IN ('Z-FIT01', 'Z-FIT02') ORDER BY stat_hour DESC, metric_code LIMIT ?", new Object[]{n2 * 2});
        }
        return this.jdbcTemplate.queryForList("SELECT metric_code, metric_name, stat_hour, value_decimal, sample_count FROM monitor_metric_hourly WHERE metric_code = ? ORDER BY stat_hour DESC LIMIT ?", new Object[]{string, n2});
    }

    public List<Map<String, Object>> listMetricDaily(String string, int n) {
        int n2 = this.normalizeLimit(n, 1, 1000, 31);
        if ("INFLOW_TOTAL_DAILY_AVG".equals(string)) {
            return this.jdbcTemplate.queryForList("SELECT metric_code, metric_name, stat_date, value_decimal, sample_count FROM monitor_metric_daily WHERE metric_code IN ('Z-FIT01', 'Z-FIT02') ORDER BY stat_date DESC, metric_code LIMIT ?", new Object[]{n2 * 2});
        }
        return this.jdbcTemplate.queryForList("SELECT metric_code, metric_name, stat_date, value_decimal, sample_count FROM monitor_metric_daily WHERE metric_code = ? ORDER BY stat_date DESC LIMIT ?", new Object[]{string, n2});
    }

    public Map<String, Object> latestRealtimeMetric(String string) {
        List list = this.jdbcTemplate.queryForList("SELECT 'INFLOW_TOTALIZER' AS metric_code, '\u4e00\u4e8c\u671f\u8fdb\u6c34\u5408\u8ba1\u7d2f\u8ba1\u6d41\u91cf' AS metric_name, collect_time, total_value AS value_decimal FROM monitor_totalizer_snapshot ORDER BY collect_time DESC LIMIT 1");
        return list.isEmpty() ? null : (Map)list.get(0);
    }

    private int normalizeLimit(int n, int n2, int n3, int n4) {
        if (n <= 0) {
            return n4;
        }
        if (n < n2) {
            return n2;
        }
        if (n > n3) {
            return n3;
        }
        return n;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private MonitorPointConfig mapPointConfig(ResultSet resultSet) throws SQLException {
        MonitorPointConfig monitorPointConfig = new MonitorPointConfig();
        monitorPointConfig.setId(resultSet.getLong("id"));
        monitorPointConfig.setPointCode(resultSet.getString("point_code"));
        monitorPointConfig.setPointName(resultSet.getString("point_name"));
        monitorPointConfig.setAreaName(resultSet.getString("area_name"));
        monitorPointConfig.setCategoryName(resultSet.getString("category_name"));
        monitorPointConfig.setObjectName(resultSet.getString("object_name"));
        monitorPointConfig.setInstrumentName(resultSet.getString("instrument_name"));
        monitorPointConfig.setUnit(resultSet.getString("unit"));
        monitorPointConfig.setRawSaveCycleType(resultSet.getString("raw_save_cycle_type"));
        monitorPointConfig.setRawSaveCycleValue(resultSet.getInt("raw_save_cycle_value"));
        monitorPointConfig.setNeedHourlyAvg(resultSet.getInt("need_hourly_avg") == 1);
        monitorPointConfig.setNeedDailyAvg(resultSet.getInt("need_daily_avg") == 1);
        monitorPointConfig.setNeedTotalizer(resultSet.getInt("need_totalizer") == 1);
        return monitorPointConfig;
    }

    public static class MonitorPointConfig {
        private long id;
        private String pointCode;
        private String pointName;
        private String areaName;
        private String categoryName;
        private String objectName;
        private String instrumentName;
        private String unit;
        private String rawSaveCycleType;
        private int rawSaveCycleValue;
        private boolean needHourlyAvg;
        private boolean needDailyAvg;
        private boolean needTotalizer;

        public long getId() {
            return this.id;
        }

        public void setId(long l) {
            this.id = l;
        }

        public String getPointCode() {
            return this.pointCode;
        }

        public void setPointCode(String string) {
            this.pointCode = string;
        }

        public String getPointName() {
            return this.pointName;
        }

        public void setPointName(String string) {
            this.pointName = string;
        }

        public String getAreaName() {
            return this.areaName;
        }

        public void setAreaName(String string) {
            this.areaName = string;
        }

        public String getCategoryName() {
            return this.categoryName;
        }

        public void setCategoryName(String string) {
            this.categoryName = string;
        }

        public String getObjectName() {
            return this.objectName;
        }

        public void setObjectName(String string) {
            this.objectName = string;
        }

        public String getInstrumentName() {
            return this.instrumentName;
        }

        public void setInstrumentName(String string) {
            this.instrumentName = string;
        }

        public String getUnit() {
            return this.unit;
        }

        public void setUnit(String string) {
            this.unit = string;
        }

        public String getRawSaveCycleType() {
            return this.rawSaveCycleType;
        }

        public void setRawSaveCycleType(String string) {
            this.rawSaveCycleType = string;
        }

        public int getRawSaveCycleValue() {
            return this.rawSaveCycleValue;
        }

        public void setRawSaveCycleValue(int n) {
            this.rawSaveCycleValue = n;
        }

        public boolean isNeedHourlyAvg() {
            return this.needHourlyAvg;
        }

        public void setNeedHourlyAvg(boolean bl) {
            this.needHourlyAvg = bl;
        }

        public boolean isNeedDailyAvg() {
            return this.needDailyAvg;
        }

        public void setNeedDailyAvg(boolean bl) {
            this.needDailyAvg = bl;
        }

        public boolean isNeedTotalizer() {
            return this.needTotalizer;
        }

        public void setNeedTotalizer(boolean bl) {
            this.needTotalizer = bl;
        }
    }

    public static class DeviceFaultState {
        private final boolean faultStatus;
        private final LocalDateTime lastChangeTime;

        public DeviceFaultState(boolean bl, LocalDateTime localDateTime) {
            this.faultStatus = bl;
            this.lastChangeTime = localDateTime;
        }

        public boolean isFaultStatus() {
            return this.faultStatus;
        }

        public LocalDateTime getLastChangeTime() {
            return this.lastChangeTime;
        }
    }

    private static final class RealtimeMetricState {
        private final LocalDateTime collectTime;
        private final BigDecimal value;

        private RealtimeMetricState(LocalDateTime localDateTime, BigDecimal bigDecimal) {
            this.collectTime = localDateTime;
            this.value = bigDecimal;
        }
    }
}

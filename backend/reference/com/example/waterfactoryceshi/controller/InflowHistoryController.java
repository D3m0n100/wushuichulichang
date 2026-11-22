/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.http.HttpStatus
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.server.ResponseStatusException
 */
package com.example.waterfactoryceshi.controller;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value={"/api/monitor/metrics/inflow/history"})
@ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
public class InflowHistoryController {
    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_BUCKETS = 10000;
    private static final String INSTANTANEOUS_SQL = "SELECT DATE_FORMAT(bucket_time, '%Y-%m-%d %H:%i:%s') AS collect_time, ROUND(AVG(CASE WHEN point_code = 'Z-FIT01' THEN value_decimal END), 3) AS phase1_instant, ROUND(AVG(CASE WHEN point_code = 'Z-FIT02' THEN value_decimal END), 3) AS phase2_instant, CASE WHEN COUNT(CASE WHEN point_code = 'Z-FIT01' THEN 1 END) > 0 AND COUNT(CASE WHEN point_code = 'Z-FIT02' THEN 1 END) > 0 THEN ROUND(AVG(CASE WHEN point_code = 'Z-FIT01' THEN value_decimal END) + AVG(CASE WHEN point_code = 'Z-FIT02' THEN value_decimal END), 3) ELSE NULL END AS instant_sum FROM (SELECT point_code, value_decimal, FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(collect_time) / ?) * ?) AS bucket_time FROM monitor_point_data WHERE point_code IN ('Z-FIT01', 'Z-FIT02') AND collect_time >= ? AND collect_time <= ?) source GROUP BY bucket_time ORDER BY bucket_time ASC";
    private static final String HOURLY_METRIC_SQL = "SELECT DATE_FORMAT(stat_hour, '%Y-%m-%d %H:%i:%s') AS collect_time, ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END), 3) AS phase1_instant, ROUND(MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) AS phase2_instant, CASE WHEN COUNT(CASE WHEN metric_code = 'Z-FIT01' THEN 1 END) > 0 AND COUNT(CASE WHEN metric_code = 'Z-FIT02' THEN 1 END) > 0 THEN ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END) + MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) ELSE NULL END AS instant_sum FROM monitor_metric_hourly WHERE metric_code IN ('Z-FIT01', 'Z-FIT02') AND stat_hour >= ? AND stat_hour <= ? GROUP BY stat_hour ORDER BY stat_hour ASC";
    private static final String DAILY_METRIC_SQL = "SELECT DATE_FORMAT(stat_date, '%Y-%m-%d 00:00:00') AS collect_time, ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END), 3) AS phase1_instant, ROUND(MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) AS phase2_instant, CASE WHEN COUNT(CASE WHEN metric_code = 'Z-FIT01' THEN 1 END) > 0 AND COUNT(CASE WHEN metric_code = 'Z-FIT02' THEN 1 END) > 0 THEN ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END) + MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) ELSE NULL END AS instant_sum FROM monitor_metric_daily WHERE metric_code IN ('Z-FIT01', 'Z-FIT02') AND stat_date >= ? AND stat_date <= ? GROUP BY stat_date ORDER BY stat_date ASC";
    private static final String TOTALIZER_SQL = "WITH combined AS (SELECT 'phase1' AS series_code, id, collect_time, total_value FROM monitor_phase1_totalizer WHERE collect_time >= ? AND collect_time <= ? UNION ALL SELECT 'phase2' AS series_code, id, collect_time, total_value FROM monitor_phase2_totalizer WHERE collect_time >= ? AND collect_time <= ? UNION ALL SELECT 'sum' AS series_code, id, collect_time, total_value FROM monitor_totalizer_snapshot WHERE collect_time >= ? AND collect_time <= ?), bucketed AS (SELECT series_code, id, collect_time, total_value, FLOOR(UNIX_TIMESTAMP(collect_time) / ?) AS bucket_id FROM combined), ranked AS (SELECT series_code, collect_time, total_value, bucket_id, ROW_NUMBER() OVER (PARTITION BY series_code, bucket_id ORDER BY collect_time DESC, id DESC) AS row_num FROM bucketed) SELECT DATE_FORMAT(FROM_UNIXTIME(bucket_id * ?), '%Y-%m-%d %H:%i:%s') AS collect_time, DATE_FORMAT(MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN collect_time END), '%Y-%m-%d %H:%i:%s') AS phase1_totalizer_time, DATE_FORMAT(MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN collect_time END), '%Y-%m-%d %H:%i:%s') AS phase2_totalizer_time, DATE_FORMAT(MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN collect_time END), '%Y-%m-%d %H:%i:%s') AS totalizer_sum_time, MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN total_value END) AS phase1_totalizer, MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN total_value END) AS phase2_totalizer, MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN total_value END) AS totalizer_sum FROM ranked WHERE row_num = 1 GROUP BY bucket_id ORDER BY bucket_id ASC";
    private static final String DAILY_TOTALIZER_SQL = "WITH combined AS (SELECT 'phase1' AS series_code, id, collect_time, total_value FROM monitor_phase1_totalizer WHERE collect_time >= ? AND collect_time <= ? UNION ALL SELECT 'phase2' AS series_code, id, collect_time, total_value FROM monitor_phase2_totalizer WHERE collect_time >= ? AND collect_time <= ? UNION ALL SELECT 'sum' AS series_code, id, collect_time, total_value FROM monitor_totalizer_snapshot WHERE collect_time >= ? AND collect_time <= ?), ranked AS (SELECT series_code, collect_time, total_value, DATE(collect_time) AS bucket_date, ROW_NUMBER() OVER (PARTITION BY series_code, DATE(collect_time) ORDER BY collect_time DESC, id DESC) AS row_num FROM combined) SELECT DATE_FORMAT(bucket_date, '%Y-%m-%d 00:00:00') AS collect_time, DATE_FORMAT(MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN collect_time END), '%Y-%m-%d %H:%i:%s') AS phase1_totalizer_time, DATE_FORMAT(MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN collect_time END), '%Y-%m-%d %H:%i:%s') AS phase2_totalizer_time, DATE_FORMAT(MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN collect_time END), '%Y-%m-%d %H:%i:%s') AS totalizer_sum_time, MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN total_value END) AS phase1_totalizer, MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN total_value END) AS phase2_totalizer, MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN total_value END) AS totalizer_sum FROM ranked WHERE row_num = 1 GROUP BY bucket_date ORDER BY bucket_date ASC";
    private final JdbcTemplate jdbcTemplate;

    public InflowHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> history(@RequestParam(name="start") String string, @RequestParam(name="end") String string2, @RequestParam(name="intervalMinutes", defaultValue="5") int n, @RequestParam(name="resolution", defaultValue="minute") String string3) {
        List list;
        int n2;
        int n3;
        String string4;
        LocalDateTime localDateTime = this.parseDateTime(string);
        LocalDateTime localDateTime2 = this.parseDateTime(string2);
        if (localDateTime2.isBefore(localDateTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must not be before start");
        }
        String string5 = string4 = string3 == null ? "minute" : string3.trim().toLowerCase(Locale.ROOT);
        if ("raw".equals(string4)) {
            string4 = "minute";
        } else if ("hour".equals(string4)) {
            string4 = "hourly";
        } else if ("day".equals(string4)) {
            string4 = "daily";
        }
        if (!("minute".equals(string4) || "hourly".equals(string4) || "daily".equals(string4))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolution must be minute, hourly, or daily");
        }
        Timestamp timestamp = Timestamp.valueOf(localDateTime);
        Timestamp timestamp2 = Timestamp.valueOf(localDateTime2);
        if ("hourly".equals(string4)) {
            n3 = 60;
            n2 = 3600;
            long l = ChronoUnit.HOURS.between(localDateTime.truncatedTo(ChronoUnit.HOURS), localDateTime2.truncatedTo(ChronoUnit.HOURS)) + 1L;
            this.ensureBucketLimit(l);
            list = this.jdbcTemplate.queryForList(HOURLY_METRIC_SQL, new Object[]{Timestamp.valueOf(localDateTime.truncatedTo(ChronoUnit.HOURS)), Timestamp.valueOf(localDateTime2.truncatedTo(ChronoUnit.HOURS))});
        } else if ("daily".equals(string4)) {
            n3 = 1440;
            n2 = 86400;
            long l = ChronoUnit.DAYS.between(localDateTime.toLocalDate(), localDateTime2.toLocalDate()) + 1L;
            this.ensureBucketLimit(l);
            list = this.jdbcTemplate.queryForList(DAILY_METRIC_SQL, new Object[]{Date.valueOf(localDateTime.toLocalDate()), Date.valueOf(localDateTime2.toLocalDate())});
        } else {
            n3 = Math.max(1, Math.min(n, 1440));
            n2 = n3 * 60;
            long l = Math.max(1L, Duration.between(localDateTime, localDateTime2).toMinutes() + 1L);
            long l2 = (l + (long)n3 - 1L) / (long)n3;
            this.ensureBucketLimit(l2);
            list = this.jdbcTemplate.queryForList(INSTANTANEOUS_SQL, new Object[]{n2, n2, timestamp, timestamp2});
        }
        List list2 = "daily".equals(string4) ? this.jdbcTemplate.queryForList(DAILY_TOTALIZER_SQL, new Object[]{timestamp, timestamp2, timestamp, timestamp2, timestamp, timestamp2}) : this.jdbcTemplate.queryForList(TOTALIZER_SQL, new Object[]{timestamp, timestamp2, timestamp, timestamp2, timestamp, timestamp2, n2, n2});
        TreeMap<String, Map<String, Object>> treeMap = new TreeMap<String, Map<String, Object>>();
        this.mergeRows(treeMap, list, "phase1_instant", "phase2_instant", "instant_sum");
        this.mergeRows(treeMap, list2, "phase1_totalizer", "phase2_totalizer", "totalizer_sum", "phase1_totalizer_time", "phase2_totalizer_time", "totalizer_sum_time");
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        linkedHashMap.put("startTime", localDateTime.format(OUTPUT_FORMAT));
        linkedHashMap.put("endTime", localDateTime2.format(OUTPUT_FORMAT));
        linkedHashMap.put("intervalMinutes", n3);
        linkedHashMap.put("resolution", string4);
        linkedHashMap.put("rows", new ArrayList<Map<String, Object>>(treeMap.values()));
        return linkedHashMap;
    }

    private void ensureBucketLimit(long l) {
        if (l > 10000L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "time range is too large for the selected resolution; maximum buckets: 10000");
        }
    }

    private void mergeRows(TreeMap<String, Map<String, Object>> treeMap, List<Map<String, Object>> list, String ... stringArray) {
        for (Map<String, Object> map : list) {
            String string = String.valueOf(this.readValue(map, "collect_time"));
            if (string == null || string.length() == 0 || "null".equalsIgnoreCase(string)) continue;
            Map<String, Object> map2 = treeMap.get(string);
            if (map2 == null) {
                map2 = new LinkedHashMap<String, Object>();
                map2.put("collect_time", string);
                treeMap.put(string, map2);
            }
            for (String string2 : stringArray) {
                map2.put(string2, this.readValue(map, string2));
            }
        }
    }

    private Object readValue(Map<String, Object> map, String string) {
        Object object = map.get(string);
        if (object != null || map.containsKey(string)) {
            return object;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!string.equalsIgnoreCase(entry.getKey())) continue;
            return entry.getValue();
        }
        return null;
    }

    private LocalDateTime parseDateTime(String string) {
        if (string == null || string.trim().length() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start and end are required");
        }
        String string2 = string.trim();
        try {
            return LocalDateTime.parse(string2, SPACE_FORMAT);
        }
        catch (DateTimeParseException dateTimeParseException) {
            try {
                return LocalDateTime.parse(string2, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            catch (DateTimeParseException dateTimeParseException2) {
                try {
                    return LocalDateTime.parse(string2, MINUTE_FORMAT);
                }
                catch (DateTimeParseException dateTimeParseException3) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid date-time format; use yyyy-MM-dd HH:mm:ss");
                }
            }
        }
    }
}

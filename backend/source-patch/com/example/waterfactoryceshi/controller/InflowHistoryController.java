package com.example.waterfactoryceshi.controller;

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
@RequestMapping("/api/monitor/metrics/inflow/history")
@ConditionalOnProperty(prefix = "monitor.db", name = "enabled", havingValue = "true")
public class InflowHistoryController {
    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_BUCKETS = 10000;

    private static final String INSTANTANEOUS_SQL =
            "SELECT DATE_FORMAT(bucket_time, '%Y-%m-%d %H:%i:%s') AS collect_time, "
                    + "ROUND(AVG(CASE WHEN point_code = 'Z-FIT01' THEN value_decimal END), 3) AS phase1_instant, "
                    + "ROUND(AVG(CASE WHEN point_code = 'Z-FIT02' THEN value_decimal END), 3) AS phase2_instant, "
                    + "CASE WHEN COUNT(CASE WHEN point_code = 'Z-FIT01' THEN 1 END) > 0 "
                    + "AND COUNT(CASE WHEN point_code = 'Z-FIT02' THEN 1 END) > 0 "
                    + "THEN ROUND(AVG(CASE WHEN point_code = 'Z-FIT01' THEN value_decimal END) "
                    + "+ AVG(CASE WHEN point_code = 'Z-FIT02' THEN value_decimal END), 3) ELSE NULL END AS instant_sum "
                    + "FROM (SELECT point_code, value_decimal, "
                    + "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(collect_time) / ?) * ?) AS bucket_time "
                    + "FROM monitor_point_data WHERE point_code IN ('Z-FIT01', 'Z-FIT02') "
                    + "AND collect_time >= ? AND collect_time <= ?) source "
                    + "GROUP BY bucket_time ORDER BY bucket_time ASC";

    private static final String HOURLY_METRIC_SQL =
            "SELECT DATE_FORMAT(stat_hour, '%Y-%m-%d %H:%i:%s') AS collect_time, "
                    + "ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END), 3) AS phase1_instant, "
                    + "ROUND(MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) AS phase2_instant, "
                    + "CASE WHEN COUNT(CASE WHEN metric_code = 'Z-FIT01' THEN 1 END) > 0 "
                    + "AND COUNT(CASE WHEN metric_code = 'Z-FIT02' THEN 1 END) > 0 "
                    + "THEN ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END) "
                    + "+ MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) ELSE NULL END AS instant_sum "
                    + "FROM monitor_metric_hourly WHERE metric_code IN ('Z-FIT01', 'Z-FIT02') "
                    + "AND stat_hour >= ? AND stat_hour <= ? "
                    + "GROUP BY stat_hour ORDER BY stat_hour ASC";

    private static final String DAILY_METRIC_SQL =
            "SELECT DATE_FORMAT(stat_date, '%Y-%m-%d 00:00:00') AS collect_time, "
                    + "ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END), 3) AS phase1_instant, "
                    + "ROUND(MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) AS phase2_instant, "
                    + "CASE WHEN COUNT(CASE WHEN metric_code = 'Z-FIT01' THEN 1 END) > 0 "
                    + "AND COUNT(CASE WHEN metric_code = 'Z-FIT02' THEN 1 END) > 0 "
                    + "THEN ROUND(MAX(CASE WHEN metric_code = 'Z-FIT01' THEN value_decimal END) "
                    + "+ MAX(CASE WHEN metric_code = 'Z-FIT02' THEN value_decimal END), 3) ELSE NULL END AS instant_sum "
                    + "FROM monitor_metric_daily WHERE metric_code IN ('Z-FIT01', 'Z-FIT02') "
                    + "AND stat_date >= ? AND stat_date <= ? "
                    + "GROUP BY stat_date ORDER BY stat_date ASC";

    private static final String TOTALIZER_SQL =
            "WITH combined AS ("
                    + "SELECT 'phase1' AS series_code, id, collect_time, total_value "
                    + "FROM monitor_phase1_totalizer WHERE collect_time >= ? AND collect_time <= ? "
                    + "UNION ALL SELECT 'phase2' AS series_code, id, collect_time, total_value "
                    + "FROM monitor_phase2_totalizer WHERE collect_time >= ? AND collect_time <= ? "
                    + "UNION ALL SELECT 'sum' AS series_code, id, collect_time, total_value "
                    + "FROM monitor_totalizer_snapshot WHERE collect_time >= ? AND collect_time <= ?), "
                    + "bucketed AS (SELECT series_code, id, collect_time, total_value, "
                    + "FLOOR(UNIX_TIMESTAMP(collect_time) / ?) AS bucket_id FROM combined), "
                    + "ranked AS (SELECT series_code, collect_time, total_value, bucket_id, "
                    + "ROW_NUMBER() OVER (PARTITION BY series_code, bucket_id "
                    + "ORDER BY collect_time DESC, id DESC) AS row_num FROM bucketed) "
                    + "SELECT DATE_FORMAT(FROM_UNIXTIME(bucket_id * ?), '%Y-%m-%d %H:%i:%s') AS collect_time, "
                    + "DATE_FORMAT(MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN collect_time END), "
                    + "'%Y-%m-%d %H:%i:%s') AS phase1_totalizer_time, "
                    + "DATE_FORMAT(MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN collect_time END), "
                    + "'%Y-%m-%d %H:%i:%s') AS phase2_totalizer_time, "
                    + "DATE_FORMAT(MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN collect_time END), "
                    + "'%Y-%m-%d %H:%i:%s') AS totalizer_sum_time, "
                    + "MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN total_value END) AS phase1_totalizer, "
                    + "MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN total_value END) AS phase2_totalizer, "
                    + "MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN total_value END) AS totalizer_sum "
                    + "FROM ranked WHERE row_num = 1 GROUP BY bucket_id ORDER BY bucket_id ASC";

    private static final String DAILY_TOTALIZER_SQL =
            "WITH combined AS ("
                    + "SELECT 'phase1' AS series_code, id, collect_time, total_value "
                    + "FROM monitor_phase1_totalizer WHERE collect_time >= ? AND collect_time <= ? "
                    + "UNION ALL SELECT 'phase2' AS series_code, id, collect_time, total_value "
                    + "FROM monitor_phase2_totalizer WHERE collect_time >= ? AND collect_time <= ? "
                    + "UNION ALL SELECT 'sum' AS series_code, id, collect_time, total_value "
                    + "FROM monitor_totalizer_snapshot WHERE collect_time >= ? AND collect_time <= ?), "
                    + "ranked AS (SELECT series_code, collect_time, total_value, DATE(collect_time) AS bucket_date, "
                    + "ROW_NUMBER() OVER (PARTITION BY series_code, DATE(collect_time) "
                    + "ORDER BY collect_time DESC, id DESC) AS row_num FROM combined) "
                    + "SELECT DATE_FORMAT(bucket_date, '%Y-%m-%d 00:00:00') AS collect_time, "
                    + "DATE_FORMAT(MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN collect_time END), "
                    + "'%Y-%m-%d %H:%i:%s') AS phase1_totalizer_time, "
                    + "DATE_FORMAT(MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN collect_time END), "
                    + "'%Y-%m-%d %H:%i:%s') AS phase2_totalizer_time, "
                    + "DATE_FORMAT(MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN collect_time END), "
                    + "'%Y-%m-%d %H:%i:%s') AS totalizer_sum_time, "
                    + "MAX(CASE WHEN series_code = 'phase1' AND row_num = 1 THEN total_value END) AS phase1_totalizer, "
                    + "MAX(CASE WHEN series_code = 'phase2' AND row_num = 1 THEN total_value END) AS phase2_totalizer, "
                    + "MAX(CASE WHEN series_code = 'sum' AND row_num = 1 THEN total_value END) AS totalizer_sum "
                    + "FROM ranked WHERE row_num = 1 GROUP BY bucket_date ORDER BY bucket_date ASC";

    private final JdbcTemplate jdbcTemplate;

    public InflowHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> history(
            @RequestParam(name = "start") String startValue,
            @RequestParam(name = "end") String endValue,
            @RequestParam(name = "intervalMinutes", defaultValue = "5") int intervalMinutes,
            @RequestParam(name = "resolution", defaultValue = "minute") String resolutionValue) {
        LocalDateTime start = parseDateTime(startValue);
        LocalDateTime end = parseDateTime(endValue);

        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must not be before start");
        }

        String resolution = resolutionValue == null
                ? "minute"
                : resolutionValue.trim().toLowerCase(Locale.ROOT);
        if ("raw".equals(resolution)) {
            resolution = "minute";
        } else if ("hour".equals(resolution)) {
            resolution = "hourly";
        } else if ("day".equals(resolution)) {
            resolution = "daily";
        }
        if (!"minute".equals(resolution) && !"hourly".equals(resolution) && !"daily".equals(resolution)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "resolution must be minute, hourly, or daily");
        }

        int safeInterval;
        int bucketSeconds;
        long bucketCount;
        List<Map<String, Object>> instantaneousRows;
        Timestamp startTimestamp = Timestamp.valueOf(start);
        Timestamp endTimestamp = Timestamp.valueOf(end);

        if ("hourly".equals(resolution)) {
            safeInterval = 60;
            bucketSeconds = 60 * 60;
            bucketCount = ChronoUnit.HOURS.between(
                    start.truncatedTo(ChronoUnit.HOURS),
                    end.truncatedTo(ChronoUnit.HOURS)) + 1L;
            ensureBucketLimit(bucketCount);
            instantaneousRows = jdbcTemplate.queryForList(
                    HOURLY_METRIC_SQL,
                    Timestamp.valueOf(start.truncatedTo(ChronoUnit.HOURS)),
                    Timestamp.valueOf(end.truncatedTo(ChronoUnit.HOURS)));
        } else if ("daily".equals(resolution)) {
            safeInterval = 1440;
            bucketSeconds = 24 * 60 * 60;
            bucketCount = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1L;
            ensureBucketLimit(bucketCount);
            instantaneousRows = jdbcTemplate.queryForList(
                    DAILY_METRIC_SQL,
                    java.sql.Date.valueOf(start.toLocalDate()),
                    java.sql.Date.valueOf(end.toLocalDate()));
        } else {
            safeInterval = Math.max(1, Math.min(intervalMinutes, 1440));
            bucketSeconds = safeInterval * 60;
            long spanMinutes = Math.max(1L, Duration.between(start, end).toMinutes() + 1L);
            bucketCount = (spanMinutes + safeInterval - 1L) / safeInterval;
            ensureBucketLimit(bucketCount);
            instantaneousRows = jdbcTemplate.queryForList(
                    INSTANTANEOUS_SQL,
                    bucketSeconds,
                    bucketSeconds,
                    startTimestamp,
                    endTimestamp);
        }

        List<Map<String, Object>> totalizerRows;
        if ("daily".equals(resolution)) {
            totalizerRows = jdbcTemplate.queryForList(
                    DAILY_TOTALIZER_SQL,
                    startTimestamp,
                    endTimestamp,
                    startTimestamp,
                    endTimestamp,
                    startTimestamp,
                    endTimestamp);
        } else {
            totalizerRows = jdbcTemplate.queryForList(
                    TOTALIZER_SQL,
                    startTimestamp,
                    endTimestamp,
                    startTimestamp,
                    endTimestamp,
                    startTimestamp,
                    endTimestamp,
                    bucketSeconds,
                    bucketSeconds);
        }

        TreeMap<String, Map<String, Object>> mergedRows = new TreeMap<String, Map<String, Object>>();
        mergeRows(mergedRows, instantaneousRows, "phase1_instant", "phase2_instant", "instant_sum");
        mergeRows(
                mergedRows,
                totalizerRows,
                "phase1_totalizer",
                "phase2_totalizer",
                "totalizer_sum",
                "phase1_totalizer_time",
                "phase2_totalizer_time",
                "totalizer_sum_time");

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("startTime", start.format(OUTPUT_FORMAT));
        result.put("endTime", end.format(OUTPUT_FORMAT));
        result.put("intervalMinutes", safeInterval);
        result.put("resolution", resolution);
        result.put("rows", new ArrayList<Map<String, Object>>(mergedRows.values()));
        return result;
    }

    private void ensureBucketLimit(long bucketCount) {
        if (bucketCount > MAX_BUCKETS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "time range is too large for the selected resolution; maximum buckets: " + MAX_BUCKETS);
        }
    }

    private void mergeRows(
            TreeMap<String, Map<String, Object>> target,
            List<Map<String, Object>> source,
            String... valueKeys) {
        for (Map<String, Object> row : source) {
            String collectTime = String.valueOf(readValue(row, "collect_time"));
            if (collectTime == null || collectTime.length() == 0 || "null".equalsIgnoreCase(collectTime)) {
                continue;
            }
            Map<String, Object> merged = target.get(collectTime);
            if (merged == null) {
                merged = new LinkedHashMap<String, Object>();
                merged.put("collect_time", collectTime);
                target.put(collectTime, merged);
            }
            for (String valueKey : valueKeys) {
                merged.put(valueKey, readValue(row, valueKey));
            }
        }
    }

    private Object readValue(Map<String, Object> row, String key) {
        Object direct = row.get(key);
        if (direct != null || row.containsKey(key)) {
            return direct;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().length() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start and end are required");
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed, SPACE_FORMAT);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    return LocalDateTime.parse(trimmed, MINUTE_FORMAT);
                } catch (DateTimeParseException finalFailure) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "invalid date-time format; use yyyy-MM-dd HH:mm:ss");
                }
            }
        }
    }
}

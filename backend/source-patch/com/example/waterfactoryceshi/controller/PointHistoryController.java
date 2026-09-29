package com.example.waterfactoryceshi.controller;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/monitor/points")
@ConditionalOnProperty(prefix = "monitor.db", name = "enabled", havingValue = "true")
public class PointHistoryController {
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ROWS = 10000;

    private static final String MINUTE_SQL =
            "SELECT point_code, DATE_FORMAT(bucket_time, '%Y-%m-%d %H:%i:%s') AS collect_time, "
                    + "ROUND(AVG(value_decimal), 3) AS value_decimal, MIN(value_decimal) AS min_value, "
                    + "MAX(value_decimal) AS max_value, COUNT(*) AS sample_count "
                    + "FROM (SELECT point_code, value_decimal, "
                    + "FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(collect_time) / ?) * ?) AS bucket_time "
                    + "FROM monitor_point_data WHERE point_code = ? AND collect_time >= ? AND collect_time <= ? "
                    + "AND value_decimal IS NOT NULL) source "
                    + "GROUP BY point_code, bucket_time ORDER BY bucket_time ASC LIMIT " + MAX_ROWS;

    private static final String HOURLY_SQL =
            "SELECT point_code, stat_hour, avg_value, min_value, max_value, sample_count "
                    + "FROM monitor_point_hourly WHERE point_code = ? AND stat_hour >= ? AND stat_hour <= ? "
                    + "ORDER BY stat_hour ASC LIMIT " + MAX_ROWS;

    private static final String DAILY_SQL =
            "SELECT point_code, stat_date, avg_value, min_value, max_value, sample_count "
                    + "FROM monitor_point_daily WHERE point_code = ? AND stat_date >= ? AND stat_date <= ? "
                    + "ORDER BY stat_date ASC LIMIT " + MAX_ROWS;

    private final JdbcTemplate jdbcTemplate;

    public PointHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(ResponseStatusException exception) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", exception.getStatus().value());
        body.put("error", exception.getStatus().getReasonPhrase());
        body.put("message", exception.getReason());
        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    @GetMapping("/{pointCode}/range")
    public List<Map<String, Object>> pointRange(
            @PathVariable("pointCode") String pointCode,
            @RequestParam(name = "start") String startValue,
            @RequestParam(name = "end") String endValue,
            @RequestParam(name = "resolution", defaultValue = "hourly") String resolutionValue,
            @RequestParam(name = "intervalMinutes", defaultValue = "5") int intervalMinutes) {
        LocalDateTime start = parseDateTime(startValue);
        LocalDateTime end = parseDateTime(endValue);
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must not be before start");
        }

        String code = pointCode == null ? "" : pointCode.trim();
        if (code.length() == 0 || code.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid point code");
        }

        String resolution = resolutionValue == null
                ? "hourly"
                : resolutionValue.trim().toLowerCase(Locale.ROOT);
        if ("minute".equals(resolution) || "raw".equals(resolution)) {
            int safeInterval = Math.max(1, Math.min(intervalMinutes, 1440));
            long spanMinutes = Math.max(1L, Duration.between(start, end).toMinutes() + 1L);
            long bucketCount = (spanMinutes + safeInterval - 1L) / safeInterval;
            ensureRowLimit(bucketCount);
            int bucketSeconds = safeInterval * 60;
            return jdbcTemplate.queryForList(
                    MINUTE_SQL,
                    bucketSeconds,
                    bucketSeconds,
                    code,
                    Timestamp.valueOf(start),
                    Timestamp.valueOf(end));
        }

        if ("hourly".equals(resolution) || "hour".equals(resolution)) {
            ensureRowLimit(Math.max(1L, Duration.between(start, end).toHours() + 1L));
            return jdbcTemplate.queryForList(
                    HOURLY_SQL,
                    code,
                    Timestamp.valueOf(start),
                    Timestamp.valueOf(end));
        }

        if ("daily".equals(resolution) || "day".equals(resolution)) {
            ensureRowLimit(Math.max(1L, Duration.between(start, end).toDays() + 1L));
            return jdbcTemplate.queryForList(
                    DAILY_SQL,
                    code,
                    Date.valueOf(start.toLocalDate()),
                    Date.valueOf(end.toLocalDate()));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolution must be minute, hourly, or daily");
    }

    private void ensureRowLimit(long rows) {
        if (rows > MAX_ROWS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "time range is too large for the selected resolution; maximum rows: " + MAX_ROWS);
        }
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

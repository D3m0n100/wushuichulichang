/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.ResponseEntity
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(value={"/api/monitor/points"})
@ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
public class PointHistoryController {
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ROWS = 10000;
    private static final String MINUTE_SQL = "SELECT point_code, DATE_FORMAT(bucket_time, '%Y-%m-%d %H:%i:%s') AS collect_time, ROUND(AVG(value_decimal), 3) AS value_decimal, MIN(value_decimal) AS min_value, MAX(value_decimal) AS max_value, COUNT(*) AS sample_count FROM (SELECT point_code, value_decimal, FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(collect_time) / ?) * ?) AS bucket_time FROM monitor_point_data WHERE point_code = ? AND collect_time >= ? AND collect_time <= ? AND value_decimal IS NOT NULL) source GROUP BY point_code, bucket_time ORDER BY bucket_time ASC LIMIT 10000";
    private static final String HOURLY_SQL = "SELECT point_code, stat_hour, avg_value, min_value, max_value, sample_count FROM monitor_point_hourly WHERE point_code = ? AND stat_hour >= ? AND stat_hour <= ? ORDER BY stat_hour ASC LIMIT 10000";
    private static final String DAILY_SQL = "SELECT point_code, stat_date, avg_value, min_value, max_value, sample_count FROM monitor_point_daily WHERE point_code = ? AND stat_date >= ? AND stat_date <= ? ORDER BY stat_date ASC LIMIT 10000";
    private final JdbcTemplate jdbcTemplate;

    public PointHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @ExceptionHandler(value={ResponseStatusException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(ResponseStatusException responseStatusException) {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        linkedHashMap.put("status", responseStatusException.getStatus().value());
        linkedHashMap.put("error", responseStatusException.getStatus().getReasonPhrase());
        linkedHashMap.put("message", responseStatusException.getReason());
        return ResponseEntity.status((HttpStatus)responseStatusException.getStatus()).body(linkedHashMap);
    }

    @GetMapping(value={"/{pointCode}/range"})
    public List<Map<String, Object>> pointRange(@PathVariable(value="pointCode") String string, @RequestParam(name="start") String string2, @RequestParam(name="end") String string3, @RequestParam(name="resolution", defaultValue="hourly") String string4, @RequestParam(name="intervalMinutes", defaultValue="5") int n) {
        String string5;
        String string6;
        LocalDateTime localDateTime = this.parseDateTime(string2);
        LocalDateTime localDateTime2 = this.parseDateTime(string3);
        if (localDateTime2.isBefore(localDateTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must not be before start");
        }
        String string7 = string6 = string == null ? "" : string.trim();
        if (string6.length() == 0 || string6.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid point code");
        }
        String string8 = string5 = string4 == null ? "hourly" : string4.trim().toLowerCase(Locale.ROOT);
        if ("minute".equals(string5) || "raw".equals(string5)) {
            int n2 = Math.max(1, Math.min(n, 1440));
            long l = Math.max(1L, Duration.between(localDateTime, localDateTime2).toMinutes() + 1L);
            long l2 = (l + (long)n2 - 1L) / (long)n2;
            this.ensureRowLimit(l2);
            int n3 = n2 * 60;
            return this.jdbcTemplate.queryForList(MINUTE_SQL, new Object[]{n3, n3, string6, Timestamp.valueOf(localDateTime), Timestamp.valueOf(localDateTime2)});
        }
        if ("hourly".equals(string5) || "hour".equals(string5)) {
            this.ensureRowLimit(Math.max(1L, Duration.between(localDateTime, localDateTime2).toHours() + 1L));
            return this.jdbcTemplate.queryForList(HOURLY_SQL, new Object[]{string6, Timestamp.valueOf(localDateTime), Timestamp.valueOf(localDateTime2)});
        }
        if ("daily".equals(string5) || "day".equals(string5)) {
            this.ensureRowLimit(Math.max(1L, Duration.between(localDateTime, localDateTime2).toDays() + 1L));
            return this.jdbcTemplate.queryForList(DAILY_SQL, new Object[]{string6, Date.valueOf(localDateTime.toLocalDate()), Date.valueOf(localDateTime2.toLocalDate())});
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolution must be minute, hourly, or daily");
    }

    private void ensureRowLimit(long l) {
        if (l > 10000L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "time range is too large for the selected resolution; maximum rows: 10000");
        }
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

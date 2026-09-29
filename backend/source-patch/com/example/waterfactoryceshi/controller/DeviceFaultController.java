package com.example.waterfactoryceshi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor/faults")
@ConditionalOnProperty(prefix = "monitor.db", name = "enabled", havingValue = "true")
public class DeviceFaultController {
    private final JdbcTemplate jdbcTemplate;

    public DeviceFaultController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/current")
    public List<Map<String, Object>> currentFaults(
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        int safeLimit = normalizeLimit(limit);
        return jdbcTemplate.queryForList(
                "SELECT id, plc_code, device_code, device_name, device_type, "
                        + "fault_status, last_change_time, last_seen_time, created_at, updated_at "
                        + "FROM monitor_device_fault_state "
                        + "WHERE fault_status = 1 "
                        + "ORDER BY last_change_time DESC, id DESC LIMIT ?",
                safeLimit);
    }

    @GetMapping("/history")
    public List<Map<String, Object>> faultHistory(
            @RequestParam(name = "limit", defaultValue = "1000") int limit) {
        int safeLimit = normalizeLimit(limit);
        return jdbcTemplate.queryForList(
                "SELECT id, plc_code, device_code, device_name, device_type, "
                        + "fault_status, event_type, event_time, created_at "
                        + "FROM monitor_device_fault_event "
                        + "ORDER BY event_time DESC, id DESC LIMIT ?",
                safeLimit);
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 2000));
    }
}

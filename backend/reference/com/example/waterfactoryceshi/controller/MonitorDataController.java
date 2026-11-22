/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.example.waterfactoryceshi.controller;

import com.example.waterfactoryceshi.service.MonitorJdbcRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/monitor"})
@ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
public class MonitorDataController {
    private static final String INFLOW_HOURLY = "INFLOW_TOTAL_HOURLY_AVG";
    private static final String INFLOW_DAILY = "INFLOW_TOTAL_DAILY_AVG";
    private static final String INFLOW_TOTALIZER = "INFLOW_TOTALIZER";
    private final MonitorJdbcRepository repository;

    public MonitorDataController(MonitorJdbcRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value={"/points"})
    public List<Map<String, Object>> points() {
        return this.repository.listPoints();
    }

    @GetMapping(value={"/points/{pointCode}/latest"})
    public List<Map<String, Object>> latestPointData(@PathVariable String pointCode, @RequestParam(defaultValue="100") int limit) {
        return this.repository.listLatestPointData(pointCode, limit);
    }

    @GetMapping(value={"/points/{pointCode}/hourly"})
    public List<Map<String, Object>> pointHourly(@PathVariable String pointCode, @RequestParam(defaultValue="24") int limit) {
        return this.repository.listPointHourly(pointCode, limit);
    }

    @GetMapping(value={"/points/{pointCode}/daily"})
    public List<Map<String, Object>> pointDaily(@PathVariable String pointCode, @RequestParam(defaultValue="31") int limit) {
        return this.repository.listPointDaily(pointCode, limit);
    }

    @GetMapping(value={"/metrics/inflow"})
    public Map<String, Object> inflowMetrics(@RequestParam(defaultValue="24") int hourlyLimit, @RequestParam(defaultValue="31") int dailyLimit) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("hourlyAverageSum", this.repository.listMetricHourly(INFLOW_HOURLY, hourlyLimit));
        result.put("dailyAverageSum", this.repository.listMetricDaily(INFLOW_DAILY, dailyLimit));
        result.put("latestTotalizerSum", this.repository.latestRealtimeMetric(INFLOW_TOTALIZER));
        return result;
    }

    @GetMapping(value={"/metrics/inflow/hourly"})
    public List<Map<String, Object>> inflowHourly(@RequestParam(defaultValue="24") int limit) {
        return this.repository.listMetricHourly(INFLOW_HOURLY, limit);
    }

    @GetMapping(value={"/metrics/inflow/daily"})
    public List<Map<String, Object>> inflowDaily(@RequestParam(defaultValue="31") int limit) {
        return this.repository.listMetricDaily(INFLOW_DAILY, limit);
    }

    @GetMapping(value={"/metrics/inflow/totalizer"})
    public Map<String, Object> inflowTotalizer() {
        return this.repository.latestRealtimeMetric(INFLOW_TOTALIZER);
    }
}

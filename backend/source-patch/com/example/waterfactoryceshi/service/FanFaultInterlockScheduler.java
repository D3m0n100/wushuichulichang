package com.example.waterfactoryceshi.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "safety.fan-fault-interlock",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FanFaultInterlockScheduler {
    private static final Logger log = LoggerFactory.getLogger(FanFaultInterlockScheduler.class);
    private static final Set<String> FAN_CODES = new HashSet<String>(Arrays.asList(
            "FAN_SYS_1",
            "FAN_SYS_2",
            "FAN_SYS_3"));

    private final FanPlcService fanPlcService;
    private final Set<String> reportedWriteFailures = new HashSet<String>();
    private boolean overviewFailureReported;

    public FanFaultInterlockScheduler(FanPlcService fanPlcService) {
        this.fanPlcService = fanPlcService;
    }

    @Scheduled(
            fixedDelayString = "${safety.fan-fault-interlock.poll-delay-ms:1000}",
            initialDelayString = "${safety.fan-fault-interlock.initial-delay-ms:5000}")
    public void clearFaultedFanEnableBits() {
        final Map<String, Object> overview;
        try {
            overview = fanPlcService.readOverview();
            if (!isFreshOverview(overview)) {
                reportOverviewFailureOnce("Fan fault interlock skipped stale or invalid overview", null);
                return;
            }
        } catch (Exception ex) {
            reportOverviewFailureOnce("Fan fault interlock failed to read fan overview", ex);
            return;
        }

        Object devicesValue = overview.get("devices");
        if (!(devicesValue instanceof Iterable<?>)) {
            reportOverviewFailureOnce("Fan fault interlock received no device list", null);
            return;
        }

        boolean supportedDeviceFound = false;
        for (Object deviceValue : (Iterable<?>) devicesValue) {
            if (!(deviceValue instanceof Map<?, ?>)) continue;
            Map<?, ?> device = (Map<?, ?>) deviceValue;
            String deviceCode = String.valueOf(device.get("code"));
            if (!FAN_CODES.contains(deviceCode)) continue;
            supportedDeviceFound = true;

            if (!shouldClearEnable(device)) {
                reportedWriteFailures.remove(deviceCode);
                continue;
            }

            try {
                boolean written = fanPlcService.writeSystemEnable(deviceCode, false);
                if (written) {
                    reportedWriteFailures.remove(deviceCode);
                    log.warn("Fan fault interlock cleared enable bit: {}", deviceCode);
                } else {
                    reportWriteFailureOnce(deviceCode, null);
                }
            } catch (Exception ex) {
                reportWriteFailureOnce(deviceCode, ex);
            }
        }

        if (supportedDeviceFound) overviewFailureReported = false;
        else reportOverviewFailureOnce("Fan fault interlock received no supported fan devices", null);
    }

    static boolean shouldClearEnable(Map<?, ?> device) {
        return isTrue(device.get("fault")) && isTrue(device.get("command"));
    }

    static boolean isFreshOverview(Map<String, Object> overview) {
        if (overview == null) return false;
        Object plcValue = overview.get("plc");
        if (!(plcValue instanceof Map<?, ?>)) return false;
        Map<?, ?> plc = (Map<?, ?>) plcValue;
        return plc.containsKey("stale") && !isTrue(plc.get("stale"));
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if (value == null) return false;
        String text = String.valueOf(value).trim();
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    private void reportOverviewFailureOnce(String message, Exception ex) {
        if (overviewFailureReported) return;
        overviewFailureReported = true;
        if (ex == null) log.warn(message);
        else log.warn(message, ex);
    }

    private void reportWriteFailureOnce(String deviceCode, Exception ex) {
        if (!reportedWriteFailures.add(deviceCode)) return;
        if (ex == null) log.error("Fan fault interlock failed to clear enable bit: {}", deviceCode);
        else log.error("Fan fault interlock failed to clear enable bit: " + deviceCode, ex);
    }
}

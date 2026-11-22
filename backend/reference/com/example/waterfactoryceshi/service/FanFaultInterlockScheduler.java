/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Component
 */
package com.example.waterfactoryceshi.service;

import com.example.waterfactoryceshi.service.FanPlcService;
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
@ConditionalOnProperty(prefix="safety.fan-fault-interlock", name={"enabled"}, havingValue="true", matchIfMissing=true)
public class FanFaultInterlockScheduler {
    private static final Logger log = LoggerFactory.getLogger(FanFaultInterlockScheduler.class);
    private static final Set<String> FAN_CODES = new HashSet<String>(Arrays.asList("FAN_SYS_1", "FAN_SYS_2", "FAN_SYS_3"));
    private final FanPlcService fanPlcService;
    private final Set<String> reportedWriteFailures = new HashSet<String>();
    private boolean overviewFailureReported;

    public FanFaultInterlockScheduler(FanPlcService fanPlcService) {
        this.fanPlcService = fanPlcService;
    }

    @Scheduled(fixedDelayString="${safety.fan-fault-interlock.poll-delay-ms:1000}", initialDelayString="${safety.fan-fault-interlock.initial-delay-ms:5000}")
    public void clearFaultedFanEnableBits() {
        Map<String, Object> map;
        try {
            map = this.fanPlcService.readOverview();
            if (!FanFaultInterlockScheduler.isFreshOverview(map)) {
                this.reportOverviewFailureOnce("Fan fault interlock skipped stale or invalid overview", null);
                return;
            }
        }
        catch (Exception exception) {
            this.reportOverviewFailureOnce("Fan fault interlock failed to read fan overview", exception);
            return;
        }
        Object object = map.get("devices");
        if (!(object instanceof Iterable)) {
            this.reportOverviewFailureOnce("Fan fault interlock received no device list", null);
            return;
        }
        boolean bl = false;
        for (Object t : (Iterable)object) {
            Map map2;
            String string;
            if (!(t instanceof Map) || !FAN_CODES.contains(string = String.valueOf((map2 = (Map)t).get("code")))) continue;
            bl = true;
            if (!FanFaultInterlockScheduler.shouldClearEnable(map2)) {
                this.reportedWriteFailures.remove(string);
                continue;
            }
            try {
                boolean bl2 = this.fanPlcService.writeSystemEnable(string, false);
                if (bl2) {
                    this.reportedWriteFailures.remove(string);
                    log.warn("Fan fault interlock cleared enable bit: {}", (Object)string);
                    continue;
                }
                this.reportWriteFailureOnce(string, null);
            }
            catch (Exception exception) {
                this.reportWriteFailureOnce(string, exception);
            }
        }
        if (bl) {
            this.overviewFailureReported = false;
        } else {
            this.reportOverviewFailureOnce("Fan fault interlock received no supported fan devices", null);
        }
    }

    static boolean shouldClearEnable(Map<?, ?> map) {
        return FanFaultInterlockScheduler.isTrue(map.get("fault")) && FanFaultInterlockScheduler.isTrue(map.get("command"));
    }

    static boolean isFreshOverview(Map<String, Object> map) {
        if (map == null) {
            return false;
        }
        Object object = map.get("plc");
        if (!(object instanceof Map)) {
            return false;
        }
        Map map2 = (Map)object;
        return map2.containsKey("stale") && !FanFaultInterlockScheduler.isTrue(map2.get("stale"));
    }

    private static boolean isTrue(Object object) {
        if (object instanceof Boolean) {
            return (Boolean)object;
        }
        if (object instanceof Number) {
            return ((Number)object).intValue() != 0;
        }
        if (object == null) {
            return false;
        }
        String string = String.valueOf(object).trim();
        return "1".equals(string) || "true".equalsIgnoreCase(string);
    }

    private void reportOverviewFailureOnce(String string, Exception exception) {
        if (this.overviewFailureReported) {
            return;
        }
        this.overviewFailureReported = true;
        if (exception == null) {
            log.warn(string);
        } else {
            log.warn(string, (Throwable)exception);
        }
    }

    private void reportWriteFailureOnce(String string, Exception exception) {
        if (!this.reportedWriteFailures.add(string)) {
            return;
        }
        if (exception == null) {
            log.error("Fan fault interlock failed to clear enable bit: {}", (Object)string);
        } else {
            log.error("Fan fault interlock failed to clear enable bit: " + string, (Throwable)exception);
        }
    }
}

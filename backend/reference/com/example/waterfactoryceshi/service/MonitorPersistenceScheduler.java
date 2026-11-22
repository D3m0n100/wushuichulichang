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

import com.example.waterfactoryceshi.service.B1PlcService;
import com.example.waterfactoryceshi.service.BaikeDosingPlcService;
import com.example.waterfactoryceshi.service.BaikeFiberDiscPlcService;
import com.example.waterfactoryceshi.service.BaikeFilterPressPlcService;
import com.example.waterfactoryceshi.service.CoarseScreenPlcService;
import com.example.waterfactoryceshi.service.DeodorizationPlcService;
import com.example.waterfactoryceshi.service.FanPlcService;
import com.example.waterfactoryceshi.service.FineScreenPlcService;
import com.example.waterfactoryceshi.service.H1PlcService;
import com.example.waterfactoryceshi.service.M3PlcService;
import com.example.waterfactoryceshi.service.MonitorPersistenceService;
import com.example.waterfactoryceshi.service.MultilayerCyclonePlcService;
import com.example.waterfactoryceshi.service.PacPlcService;
import com.example.waterfactoryceshi.service.PamPlcService;
import com.example.waterfactoryceshi.service.RotorPumpPlcService;
import com.example.waterfactoryceshi.service.SludgeSiloPlcService;
import com.example.waterfactoryceshi.service.SodiumAcetatePlcService;
import com.example.waterfactoryceshi.service.SodiumHypochloritePlcService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="monitor.db", name={"enabled"}, havingValue="true")
public class MonitorPersistenceScheduler {
    private static final Logger log = LoggerFactory.getLogger(MonitorPersistenceScheduler.class);
    private static final ZoneId MONITOR_ZONE = ZoneId.of("Asia/Shanghai");
    private final MonitorPersistenceService persistenceService;
    private final CoarseScreenPlcService coarseScreenPlcService;
    private final FineScreenPlcService fineScreenPlcService;
    private final MultilayerCyclonePlcService multilayerCyclonePlcService;
    private final PacPlcService pacPlcService;
    private final PamPlcService pamPlcService;
    private final SodiumAcetatePlcService sodiumAcetatePlcService;
    private final SodiumHypochloritePlcService sodiumHypochloritePlcService;
    private final BaikeDosingPlcService baikeDosingPlcService;
    private final BaikeFilterPressPlcService baikeFilterPressPlcService;
    private final BaikeFiberDiscPlcService baikeFiberDiscPlcService;
    private final FanPlcService fanPlcService;
    private final SludgeSiloPlcService sludgeSiloPlcService;
    private final DeodorizationPlcService deodorizationPlcService;
    private final RotorPumpPlcService rotorPumpPlcService;
    private final B1PlcService b1PlcService;
    private final H1PlcService h1PlcService;
    private final M3PlcService m3PlcService;

    public MonitorPersistenceScheduler(MonitorPersistenceService persistenceService, CoarseScreenPlcService coarseScreenPlcService, FineScreenPlcService fineScreenPlcService, MultilayerCyclonePlcService multilayerCyclonePlcService, PacPlcService pacPlcService, PamPlcService pamPlcService, SodiumAcetatePlcService sodiumAcetatePlcService, SodiumHypochloritePlcService sodiumHypochloritePlcService, BaikeDosingPlcService baikeDosingPlcService, BaikeFilterPressPlcService baikeFilterPressPlcService, BaikeFiberDiscPlcService baikeFiberDiscPlcService, FanPlcService fanPlcService, SludgeSiloPlcService sludgeSiloPlcService, DeodorizationPlcService deodorizationPlcService, RotorPumpPlcService rotorPumpPlcService, B1PlcService b1PlcService, H1PlcService h1PlcService, M3PlcService m3PlcService) {
        this.persistenceService = persistenceService;
        this.coarseScreenPlcService = coarseScreenPlcService;
        this.fineScreenPlcService = fineScreenPlcService;
        this.multilayerCyclonePlcService = multilayerCyclonePlcService;
        this.pacPlcService = pacPlcService;
        this.pamPlcService = pamPlcService;
        this.sodiumAcetatePlcService = sodiumAcetatePlcService;
        this.sodiumHypochloritePlcService = sodiumHypochloritePlcService;
        this.baikeDosingPlcService = baikeDosingPlcService;
        this.baikeFilterPressPlcService = baikeFilterPressPlcService;
        this.baikeFiberDiscPlcService = baikeFiberDiscPlcService;
        this.fanPlcService = fanPlcService;
        this.sludgeSiloPlcService = sludgeSiloPlcService;
        this.deodorizationPlcService = deodorizationPlcService;
        this.rotorPumpPlcService = rotorPumpPlcService;
        this.b1PlcService = b1PlcService;
        this.h1PlcService = h1PlcService;
        this.m3PlcService = m3PlcService;
    }

    @Scheduled(fixedDelayString="${monitor.db.poll-delay-ms:10000}")
    public void collectRealtimePoints() {
        this.persistRead("coarse-screen", () -> this.coarseScreenPlcService.readOverview());
        this.persistRead("fine-screen", () -> this.fineScreenPlcService.readOverview());
        this.persistRead("multilayer-cyclone", () -> this.multilayerCyclonePlcService.readOverview());
        this.persistRead("pac", () -> this.pacPlcService.readOverview());
        this.persistRead("pam", () -> this.pamPlcService.readOverview());
        this.persistRead("sodium-acetate", () -> this.sodiumAcetatePlcService.readOverview());
        this.persistRead("sodium-hypochlorite", () -> this.sodiumHypochloritePlcService.readOverview());
        this.persistRead("baike-dosing", () -> this.baikeDosingPlcService.readOverview());
        this.persistRead("baike-filter-press", () -> this.baikeFilterPressPlcService.readOverview());
        this.persistRead("baike-fiber-disc", () -> this.baikeFiberDiscPlcService.readOverview());
        this.persistRead("fan", () -> this.fanPlcService.readOverview());
        this.persistRead("sludge-silo", () -> this.sludgeSiloPlcService.readOverview());
        this.persistRead("deodorization", () -> this.deodorizationPlcService.readOverview());
        this.persistRead("rotor-pump", () -> this.rotorPumpPlcService.readOverview());
        this.persistRead("b1", () -> this.b1PlcService.readOverview());
        this.persistRead("h1", () -> this.h1PlcService.readOverview());
        this.persistRead("m3", () -> this.m3PlcService.readOverview());
    }

    @Scheduled(cron="0 * * * * *", zone="Asia/Shanghai")
    public void persistMinutePoints() {
        try {
            this.persistenceService.persistMinute(LocalDateTime.now(MONITOR_ZONE));
        }
        catch (Exception ex) {
            log.warn("Persist minute monitor points failed", (Throwable)ex);
        }
    }

    @Scheduled(cron="5 0 * * * *", zone="Asia/Shanghai")
    public void persistHourlyPoints() {
        try {
            this.persistenceService.persistHourly(LocalDateTime.now(MONITOR_ZONE));
        }
        catch (Exception ex) {
            log.warn("Persist hourly monitor points failed", (Throwable)ex);
        }
    }

    @Scheduled(cron="10 0 0 * * *", zone="Asia/Shanghai")
    public void persistDailyPoints() {
        try {
            this.persistenceService.persistDaily(LocalDate.now(MONITOR_ZONE));
        }
        catch (Exception ex) {
            log.warn("Persist daily monitor points failed", (Throwable)ex);
        }
    }

    private void persist(String plcCode, Map<String, Object> overview) {
        try {
            this.persistenceService.persistOverview(plcCode, overview);
        }
        catch (Exception ex) {
            this.persistenceService.invalidatePlcValues(plcCode);
            log.warn("Persist {} overview failed", (Object)plcCode, (Object)ex);
        }
    }

    private void persistRead(String plcCode, Supplier<Map<String, Object>> reader) {
        try {
            Map<String, Object> overview = reader.get();
            if (overview == null) {
                this.persistenceService.invalidatePlcValues(plcCode);
                return;
            }
            this.persist(plcCode, overview);
        }
        catch (Exception ex) {
            this.persistenceService.invalidatePlcValues(plcCode);
            log.warn("Read {} overview for monitor persistence failed", (Object)plcCode, (Object)ex);
        }
    }
}

CREATE DATABASE IF NOT EXISTS sewage_monitor DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sewage_monitor;

CREATE TABLE IF NOT EXISTS monitor_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  seq_no INT NOT NULL,
  point_code VARCHAR(64) NOT NULL UNIQUE,
  point_name VARCHAR(128) NOT NULL,
  area_name VARCHAR(64) NOT NULL,
  category_name VARCHAR(64) NOT NULL,
  object_name VARCHAR(128) NOT NULL,
  instrument_name VARCHAR(128) NOT NULL,
  unit VARCHAR(32) DEFAULT NULL,
  value_type VARCHAR(16) NOT NULL DEFAULT 'DECIMAL',
  raw_save_cycle_type VARCHAR(16) NOT NULL,
  raw_save_cycle_value INT NOT NULL DEFAULT 1,
  need_hourly_avg TINYINT NOT NULL DEFAULT 0,
  need_daily_avg TINYINT NOT NULL DEFAULT 0,
  need_totalizer TINYINT NOT NULL DEFAULT 0,
  aggregate_group VARCHAR(64) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS monitor_point_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  point_id BIGINT NOT NULL,
  point_code VARCHAR(64) NOT NULL,
  collect_time DATETIME NOT NULL,
  value_decimal DECIMAL(18,3) DEFAULT NULL,
  value_text VARCHAR(255) DEFAULT NULL,
  quality_status VARCHAR(32) NOT NULL DEFAULT 'GOOD',
  source_type VARCHAR(32) NOT NULL DEFAULT 'PLC',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_point_time (point_id, collect_time),
  UNIQUE KEY uk_point_code_time (point_code, collect_time),
  CONSTRAINT fk_monitor_point_data_point FOREIGN KEY (point_id) REFERENCES monitor_point(id)
);

CREATE TABLE IF NOT EXISTS monitor_device_fault_state (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plc_code VARCHAR(64) NOT NULL,
  device_code VARCHAR(128) NOT NULL,
  device_name VARCHAR(128) NOT NULL,
  device_type VARCHAR(64) DEFAULT NULL,
  fault_status TINYINT NOT NULL DEFAULT 0,
  last_change_time DATETIME NOT NULL,
  last_seen_time DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_device_fault_state (plc_code, device_code),
  KEY idx_fault_state_seen (last_seen_time)
);

CREATE TABLE IF NOT EXISTS monitor_device_fault_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plc_code VARCHAR(64) NOT NULL,
  device_code VARCHAR(128) NOT NULL,
  device_name VARCHAR(128) NOT NULL,
  device_type VARCHAR(64) DEFAULT NULL,
  fault_status TINYINT NOT NULL,
  event_type VARCHAR(16) NOT NULL,
  event_time DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_fault_event_time (event_time),
  KEY idx_fault_event_device_time (plc_code, device_code, event_time),
  KEY idx_fault_event_type_time (event_type, event_time)
);

CREATE TABLE IF NOT EXISTS monitor_point_hourly (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  point_id BIGINT NOT NULL,
  point_code VARCHAR(64) NOT NULL,
  stat_hour DATETIME NOT NULL,
  avg_value DECIMAL(18,3) DEFAULT NULL,
  min_value DECIMAL(18,3) DEFAULT NULL,
  max_value DECIMAL(18,3) DEFAULT NULL,
  sample_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_point_hour (point_code, stat_hour),
  KEY idx_point_hour (point_id, stat_hour),
  CONSTRAINT fk_monitor_point_hourly_point FOREIGN KEY (point_id) REFERENCES monitor_point(id)
);

CREATE TABLE IF NOT EXISTS monitor_point_daily (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  point_id BIGINT NOT NULL,
  point_code VARCHAR(64) NOT NULL,
  stat_date DATE NOT NULL,
  avg_value DECIMAL(18,3) DEFAULT NULL,
  min_value DECIMAL(18,3) DEFAULT NULL,
  max_value DECIMAL(18,3) DEFAULT NULL,
  sample_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_point_day (point_code, stat_date),
  KEY idx_point_day (point_id, stat_date),
  CONSTRAINT fk_monitor_point_daily_point FOREIGN KEY (point_id) REFERENCES monitor_point(id)
);

CREATE TABLE IF NOT EXISTS monitor_totalizer_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  collect_time DATETIME NOT NULL,
  total_value DECIMAL(20,3) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_totalizer_time (collect_time)
);

CREATE TABLE IF NOT EXISTS monitor_phase1_totalizer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  collect_time DATETIME NOT NULL,
  total_value DECIMAL(20,3) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phase1_totalizer_time (collect_time)
);

CREATE TABLE IF NOT EXISTS monitor_phase2_totalizer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  collect_time DATETIME NOT NULL,
  total_value DECIMAL(20,3) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phase2_totalizer_time (collect_time)
);

CREATE TABLE IF NOT EXISTS monitor_metric_hourly (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  metric_code VARCHAR(64) NOT NULL,
  metric_name VARCHAR(128) NOT NULL,
  stat_hour DATETIME NOT NULL,
  value_decimal DECIMAL(18,3) NOT NULL,
  sample_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_metric_hour (metric_code, stat_hour)
);

CREATE TABLE IF NOT EXISTS monitor_metric_daily (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  metric_code VARCHAR(64) NOT NULL,
  metric_name VARCHAR(128) NOT NULL,
  stat_date DATE NOT NULL,
  value_decimal DECIMAL(18,3) NOT NULL,
  sample_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_metric_day (metric_code, stat_date)
);

INSERT INTO monitor_point (
  seq_no, point_code, point_name, area_name, category_name, object_name, instrument_name, unit,
  value_type, raw_save_cycle_type, raw_save_cycle_value, need_hourly_avg, need_daily_avg,
  need_totalizer, aggregate_group, enabled
) VALUES
(1, 'Z-FIT01', 'Phase1 flow meter', 'inlet', 'flow', 'phase1 inlet flow', 'Z-FIT01', 'm3/h', 'DECIMAL', 'minute', 1, 1, 1, 1, 'INFLOW_TOTAL', 1),
(2, 'Z-FIT02', 'Phase2 flow meter', 'inlet', 'flow', 'phase2 inlet flow', 'Z-FIT02', 'm3/h', 'DECIMAL', 'minute', 1, 1, 1, 1, 'INFLOW_TOTAL', 1),
(3, 'Z-AIT01', 'Inlet SS analyzer', 'inlet', 'SS', 'inlet', 'Z-AIT01', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(4, 'Z-AIT02', 'Inlet PH analyzer', 'inlet', 'PH', 'inlet', 'Z-AIT02', 'pH', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(5, 'Z-AIT03', 'Inlet NH4 analyzer', 'inlet', 'NH4-N', 'inlet', 'Z-AIT03', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(6, 'Z-AIT04', 'Inlet COD analyzer', 'inlet', 'COD', 'inlet', 'Z-AIT04', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(7, 'Z-AIT05', 'Inlet TN analyzer', 'inlet', 'TN', 'inlet', 'Z-AIT05', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(8, 'Z-AIT06', 'Inlet TP analyzer', 'inlet', 'TP', 'inlet', 'Z-AIT06', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(9, 'H1-AIT101', 'H1-AIT101', 'aao', 'MLSS', '1 anaerobic', 'H1-AIT101', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(10, 'H1-AIT102', 'H1-AIT102', 'aao', 'ORP', '1 anaerobic', 'H1-AIT102', 'mV', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(10, 'H1-AIT103', 'H1-AIT103', 'aao', 'NH4-N', '1 anaerobic', 'H1-AIT103', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(11, 'H1-AIT104', 'H1-AIT104', 'aao', 'NO3-N', '1 anaerobic', 'H1-AIT104', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(12, 'H1-AIT105', 'H1-AIT105', 'aao', 'ORP', '1 anoxic', 'H1-AIT105', 'mV', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(13, 'H1-AIT106', 'H1-AIT106', 'aao', 'DO', '1 aerobic', 'H1-AIT106', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(14, 'H1-AIT107', 'H1-AIT107', 'aao', 'MLSS', '1 outlet', 'H1-AIT107', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(15, 'H1-AIT108', 'H1-AIT108', 'aao', 'DO', '1 outlet', 'H1-AIT108', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(16, 'H1-AIT109', 'H1-AIT109', 'aao', 'NH4-N', '1 outlet', 'H1-AIT109', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(17, 'H1-AIT110', 'H1-AIT110', 'aao', 'NO3-N', '1 outlet', 'H1-AIT110', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(18, 'H1-AIT201', 'H1-AIT201', 'aao', 'MLSS', '2 anaerobic', 'H1-AIT201', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(19, 'H1-AIT202', 'H1-AIT202', 'aao', 'ORP', '2 anaerobic', 'H1-AIT202', 'mV', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(20, 'H1-AIT203', 'H1-AIT203', 'aao', 'NH4-N', '2 anaerobic', 'H1-AIT203', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(21, 'H1-AIT204', 'H1-AIT204', 'aao', 'NO3-N', '2 anaerobic', 'H1-AIT204', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(22, 'H1-AIT205', 'H1-AIT205', 'aao', 'ORP', '2 anoxic', 'H1-AIT205', 'mV', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(23, 'H1-AIT206', 'H1-AIT206', 'aao', 'DO', '2 aerobic', 'H1-AIT206', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(24, 'H1-AIT207', 'H1-AIT207', 'aao', 'MLSS', '2 outlet', 'H1-AIT207', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(25, 'H1-AIT208', 'H1-AIT208', 'aao', 'DO', '2 outlet', 'H1-AIT208', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(26, 'H1-AIT209', 'H1-AIT209', 'aao', 'NH4-N', '2 outlet', 'H1-AIT209', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(27, 'H1-AIT210', 'H1-AIT210', 'aao', 'NO3-N', '2 outlet', 'H1-AIT210', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(28, 'H1-LIT01', 'H1-LIT01', 'secondary settling', 'sludge interface', 'tank1', 'H1-LIT01', NULL, 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(29, 'H1-LIT02', 'H1-LIT02', 'secondary settling', 'sludge interface', 'tank2', 'H1-LIT02', NULL, 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(30, 'H1-LIT03', 'H1-LIT03', 'secondary settling', 'sludge interface', 'tank3', 'H1-LIT03', NULL, 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(31, 'H1-LIT04', 'H1-LIT04', 'secondary settling', 'sludge interface', 'tank4', 'H1-LIT04', NULL, 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(32, 'H1-AIT301', 'H1-AIT301', 'secondary settling', 'sludge concentration', 'pump pit', 'H1-AIT301', NULL, 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(33, 'M3-RIT01', 'M3-RIT01', 'high-efficiency settling', 'sludge interface', 'settling tank', 'M3-RIT01', NULL, 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(34, 'M3-AIT01', 'M3-AIT01', 'high-efficiency settling', 'turbidity', 'outlet', 'M3-AIT01', 'NTU', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(35, 'T3-RIT01', 'T3-RIT01', 'sludge thickening', 'sludge interface', 'tank', 'T3-RIT01', NULL, 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(36, 'P2-AIT01', 'P2-AIT01', 'disinfection', 'chlorine', 'contact tank', 'P2-AIT01', 'mg/L', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1)
ON DUPLICATE KEY UPDATE
  point_name = VALUES(point_name),
  area_name = VALUES(area_name),
  category_name = VALUES(category_name),
  object_name = VALUES(object_name),
  instrument_name = VALUES(instrument_name),
  unit = VALUES(unit),
  raw_save_cycle_type = VALUES(raw_save_cycle_type),
  raw_save_cycle_value = VALUES(raw_save_cycle_value),
  need_hourly_avg = VALUES(need_hourly_avg),
  need_daily_avg = VALUES(need_daily_avg),
  need_totalizer = VALUES(need_totalizer),
  aggregate_group = VALUES(aggregate_group),
  enabled = VALUES(enabled);

CREATE OR REPLACE VIEW vw_inflow_hourly_total AS
SELECT stat_hour,
       ROUND(SUM(value_decimal), 3) AS total_hour_avg_value,
       SUM(sample_count) AS sample_count
FROM monitor_metric_hourly
WHERE metric_code IN ('Z-FIT01', 'Z-FIT02')
GROUP BY stat_hour;

CREATE OR REPLACE VIEW vw_inflow_daily_total AS
SELECT stat_date,
       ROUND(SUM(value_decimal), 3) AS total_day_avg_value,
       SUM(sample_count) AS sample_count
FROM monitor_metric_daily
WHERE metric_code IN ('Z-FIT01', 'Z-FIT02')
GROUP BY stat_date;

CREATE OR REPLACE VIEW vw_inflow_totalizer_latest AS
SELECT total_value AS totalizer_sum_value,
       collect_time AS latest_collect_time
FROM monitor_totalizer_snapshot
ORDER BY collect_time DESC
LIMIT 1;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_refresh_point_hourly$$
CREATE PROCEDURE sp_refresh_point_hourly(IN p_from DATETIME, IN p_to DATETIME)
BEGIN
  INSERT INTO monitor_point_hourly (point_id, point_code, stat_hour, avg_value, min_value, max_value, sample_count)
  SELECT p.id,
         d.point_code,
         STR_TO_DATE(DATE_FORMAT(d.collect_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s') AS stat_hour,
         AVG(d.value_decimal),
         MIN(d.value_decimal),
         MAX(d.value_decimal),
         COUNT(*)
  FROM monitor_point_data d
  JOIN monitor_point p ON p.point_code = d.point_code
  WHERE p.need_hourly_avg = 1
    AND d.point_code NOT IN ('Z-FIT01', 'Z-FIT02')
    AND d.value_decimal IS NOT NULL
    AND d.collect_time >= p_from
    AND d.collect_time < p_to
  GROUP BY p.id, d.point_code, STR_TO_DATE(DATE_FORMAT(d.collect_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s')
  ON DUPLICATE KEY UPDATE
    avg_value = VALUES(avg_value),
    min_value = VALUES(min_value),
    max_value = VALUES(max_value),
    sample_count = VALUES(sample_count);
END$$

DROP PROCEDURE IF EXISTS sp_refresh_point_daily$$
CREATE PROCEDURE sp_refresh_point_daily(IN p_from DATE, IN p_to DATE)
BEGIN
  INSERT INTO monitor_point_daily (point_id, point_code, stat_date, avg_value, min_value, max_value, sample_count)
  SELECT h.point_id,
         h.point_code,
         DATE(h.stat_hour),
         ROUND(AVG(h.avg_value), 3),
         MIN(h.min_value),
         MAX(h.max_value),
         SUM(h.sample_count)
  FROM monitor_point_hourly h
  WHERE h.point_code NOT IN ('Z-FIT01', 'Z-FIT02')
    AND h.avg_value IS NOT NULL
    AND DATE(h.stat_hour) >= p_from
    AND DATE(h.stat_hour) < p_to
  GROUP BY h.point_id, h.point_code, DATE(h.stat_hour)
  ON DUPLICATE KEY UPDATE
    avg_value = VALUES(avg_value),
    min_value = VALUES(min_value),
    max_value = VALUES(max_value),
    sample_count = VALUES(sample_count);
END$$

DROP PROCEDURE IF EXISTS sp_refresh_inflow_hourly_metric$$
CREATE PROCEDURE sp_refresh_inflow_hourly_metric(IN p_from DATETIME, IN p_to DATETIME)
BEGIN
  INSERT INTO monitor_metric_hourly (metric_code, metric_name, stat_hour, value_decimal, sample_count)
  SELECT d.point_code,
         CASE d.point_code WHEN 'Z-FIT01' THEN 'phase1 inflow hourly average' ELSE 'phase2 inflow hourly average' END,
         STR_TO_DATE(DATE_FORMAT(d.collect_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s'),
         ROUND(AVG(d.value_decimal), 3),
         COUNT(*)
  FROM monitor_point_data d
  WHERE d.point_code IN ('Z-FIT01', 'Z-FIT02')
    AND d.value_decimal IS NOT NULL
    AND d.collect_time >= p_from
    AND d.collect_time < p_to
  GROUP BY d.point_code, STR_TO_DATE(DATE_FORMAT(d.collect_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s')
  ON DUPLICATE KEY UPDATE
    metric_name = VALUES(metric_name),
    value_decimal = VALUES(value_decimal),
    sample_count = VALUES(sample_count);
END$$

DROP PROCEDURE IF EXISTS sp_refresh_inflow_daily_metric$$
CREATE PROCEDURE sp_refresh_inflow_daily_metric(IN p_from DATE, IN p_to DATE)
BEGIN
  INSERT INTO monitor_metric_daily (metric_code, metric_name, stat_date, value_decimal, sample_count)
  SELECT d.point_code,
         CASE d.point_code WHEN 'Z-FIT01' THEN 'phase1 inflow daily average' ELSE 'phase2 inflow daily average' END,
         DATE(d.collect_time),
         ROUND(AVG(d.value_decimal), 3),
         COUNT(*)
  FROM monitor_point_data d
  WHERE d.point_code IN ('Z-FIT01', 'Z-FIT02')
    AND d.value_decimal IS NOT NULL
    AND DATE(d.collect_time) >= p_from
    AND DATE(d.collect_time) < p_to
  GROUP BY d.point_code, DATE(d.collect_time)
  ON DUPLICATE KEY UPDATE
    metric_name = VALUES(metric_name),
    value_decimal = VALUES(value_decimal),
    sample_count = VALUES(sample_count);
END$$

DROP PROCEDURE IF EXISTS sp_refresh_all_metrics$$
CREATE PROCEDURE sp_refresh_all_metrics()
BEGIN
  DECLARE v_now DATETIME;
  SET v_now = NOW();
  CALL sp_refresh_point_hourly(DATE_SUB(v_now, INTERVAL 2 HOUR), DATE_ADD(v_now, INTERVAL 1 HOUR));
  CALL sp_refresh_point_daily(DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY));
  CALL sp_refresh_inflow_hourly_metric(DATE_SUB(v_now, INTERVAL 2 HOUR), DATE_ADD(v_now, INTERVAL 1 HOUR));
  CALL sp_refresh_inflow_daily_metric(DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY));
END$$

DELIMITER ;

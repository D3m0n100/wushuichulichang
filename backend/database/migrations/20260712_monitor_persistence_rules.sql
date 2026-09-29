CREATE TABLE IF NOT EXISTS monitor_schema_migration (
  migration_code VARCHAR(128) PRIMARY KEY,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_guard_monitor_persistence_rules$$
CREATE PROCEDURE sp_guard_monitor_persistence_rules()
BEGIN
  IF EXISTS (
    SELECT 1 FROM monitor_schema_migration
    WHERE migration_code = '20260712_monitor_persistence_rules'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'migration 20260712_monitor_persistence_rules is already applied';
  END IF;
END$$
CALL sp_guard_monitor_persistence_rules()$$
DROP PROCEDURE sp_guard_monitor_persistence_rules$$
DELIMITER ;

INSERT INTO monitor_point (
  seq_no, point_code, point_name, area_name, category_name, object_name, instrument_name, unit,
  value_type, raw_save_cycle_type, raw_save_cycle_value, need_hourly_avg, need_daily_avg,
  need_totalizer, aggregate_group, enabled
) VALUES
(10, 'H1-AIT102', 'H1-AIT102', 'aao', 'ORP', '1 anaerobic', 'H1-AIT102', 'mV', 'DECIMAL', 'hour', 1, 0, 0, 0, NULL, 1),
(9998, 'INFLOW_TOTALIZER', 'Combined inflow totalizer', 'inlet', 'totalizer', 'phase1 and phase2 inlet', 'SYSTEM', 'm3', 'DECIMAL', 'minute', 1, 0, 0, 0, 'INFLOW_TOTAL', 0)
ON DUPLICATE KEY UPDATE
  seq_no = VALUES(seq_no),
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

UPDATE monitor_point SET enabled = 0;

UPDATE monitor_point
SET enabled = 1,
    raw_save_cycle_type = CASE WHEN point_code IN ('Z-FIT01', 'Z-FIT02') THEN 'minute' ELSE 'hour' END,
    raw_save_cycle_value = 1,
    need_hourly_avg = CASE WHEN point_code IN ('Z-FIT01', 'Z-FIT02') THEN 1 ELSE 0 END,
    need_daily_avg = CASE WHEN point_code IN ('Z-FIT01', 'Z-FIT02') THEN 1 ELSE 0 END,
    need_totalizer = 0
WHERE point_code IN (
  'Z-FIT01', 'Z-FIT02',
  'Z-AIT01', 'Z-AIT02', 'Z-AIT03', 'Z-AIT04', 'Z-AIT05', 'Z-AIT06',
  'H1-AIT101', 'H1-AIT102', 'H1-AIT103', 'H1-AIT104', 'H1-AIT105',
  'H1-AIT106', 'H1-AIT107', 'H1-AIT108', 'H1-AIT109', 'H1-AIT110',
  'H1-AIT201', 'H1-AIT202', 'H1-AIT203', 'H1-AIT204', 'H1-AIT205',
  'H1-AIT206', 'H1-AIT207', 'H1-AIT208', 'H1-AIT209', 'H1-AIT210',
  'H1-LIT01', 'H1-LIT02', 'H1-LIT03', 'H1-LIT04', 'H1-AIT301',
  'M3-RIT01', 'M3-AIT01', 'T3-RIT01', 'P2-AIT01'
);

UPDATE monitor_point
SET seq_no = CASE point_code
  WHEN 'H1-AIT101' THEN 9 WHEN 'H1-AIT102' THEN 10 WHEN 'H1-AIT103' THEN 11
  WHEN 'H1-AIT104' THEN 12 WHEN 'H1-AIT105' THEN 13 WHEN 'H1-AIT106' THEN 14
  WHEN 'H1-AIT107' THEN 15 WHEN 'H1-AIT108' THEN 16 WHEN 'H1-AIT109' THEN 17
  WHEN 'H1-AIT110' THEN 18 WHEN 'H1-AIT201' THEN 19 WHEN 'H1-AIT202' THEN 20
  WHEN 'H1-AIT203' THEN 21 WHEN 'H1-AIT204' THEN 22 WHEN 'H1-AIT205' THEN 23
  WHEN 'H1-AIT206' THEN 24 WHEN 'H1-AIT207' THEN 25 WHEN 'H1-AIT208' THEN 26
  WHEN 'H1-AIT209' THEN 27 WHEN 'H1-AIT210' THEN 28 WHEN 'H1-LIT01' THEN 29
  WHEN 'H1-LIT02' THEN 30 WHEN 'H1-LIT03' THEN 31 WHEN 'H1-LIT04' THEN 32
  WHEN 'H1-AIT301' THEN 33 WHEN 'M3-RIT01' THEN 34 WHEN 'M3-AIT01' THEN 35
  WHEN 'T3-RIT01' THEN 36 WHEN 'P2-AIT01' THEN 37 ELSE seq_no END
WHERE point_code IN (
  'H1-AIT101', 'H1-AIT102', 'H1-AIT103', 'H1-AIT104', 'H1-AIT105',
  'H1-AIT106', 'H1-AIT107', 'H1-AIT108', 'H1-AIT109', 'H1-AIT110',
  'H1-AIT201', 'H1-AIT202', 'H1-AIT203', 'H1-AIT204', 'H1-AIT205',
  'H1-AIT206', 'H1-AIT207', 'H1-AIT208', 'H1-AIT209', 'H1-AIT210',
  'H1-LIT01', 'H1-LIT02', 'H1-LIT03', 'H1-LIT04', 'H1-AIT301',
  'M3-RIT01', 'M3-AIT01', 'T3-RIT01', 'P2-AIT01'
);
UPDATE monitor_point SET enabled = 0 WHERE point_code = 'INFLOW_TOTALIZER';

SET @point_index_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics
          WHERE table_schema = DATABASE() AND table_name = 'monitor_point_data'
            AND index_name = 'uk_point_collect'),
  'SELECT 1',
  'ALTER TABLE monitor_point_data ADD UNIQUE KEY uk_point_collect (point_code, collect_time)'
);
PREPARE point_index_stmt FROM @point_index_sql;
EXECUTE point_index_stmt;
DEALLOCATE PREPARE point_index_stmt;

SET @totalizer_index_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics
          WHERE table_schema = DATABASE() AND table_name = 'monitor_totalizer_snapshot'
            AND index_name = 'uk_totalizer_code_time'),
  'SELECT 1',
  'ALTER TABLE monitor_totalizer_snapshot ADD UNIQUE KEY uk_totalizer_code_time (point_code, collect_time)'
);
PREPARE totalizer_index_stmt FROM @totalizer_index_sql;
EXECUTE totalizer_index_stmt;
DEALLOCATE PREPARE totalizer_index_stmt;

CREATE TABLE IF NOT EXISTS monitor_totalizer_snapshot_backup_20260712 AS
SELECT * FROM monitor_totalizer_snapshot WHERE 1 = 0;
INSERT INTO monitor_totalizer_snapshot_backup_20260712
SELECT * FROM monitor_totalizer_snapshot
WHERE point_code IN ('Z-FIT01', 'Z-FIT02', 'INFLOW_TOTALIZER')
  AND NOT EXISTS (SELECT 1 FROM monitor_totalizer_snapshot_backup_20260712);

CREATE TABLE IF NOT EXISTS monitor_metric_realtime_backup_20260712 AS
SELECT * FROM monitor_metric_realtime WHERE 1 = 0;
INSERT INTO monitor_metric_realtime_backup_20260712
SELECT * FROM monitor_metric_realtime
WHERE metric_code = 'INFLOW_TOTALIZER'
  AND NOT EXISTS (SELECT 1 FROM monitor_metric_realtime_backup_20260712);

DELETE FROM monitor_totalizer_snapshot
WHERE point_code IN ('Z-FIT01', 'Z-FIT02', 'INFLOW_TOTALIZER');

DELETE FROM monitor_metric_realtime
WHERE metric_code = 'INFLOW_TOTALIZER';

DELETE FROM monitor_point_hourly
WHERE point_code IN ('Z-FIT01', 'Z-FIT02')
  AND stat_hour >= DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00');

DELETE FROM monitor_metric_hourly
WHERE metric_code = 'INFLOW_TOTAL_HOURLY_AVG'
  AND stat_hour >= DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00');

DELETE FROM monitor_point_daily
WHERE point_code IN ('Z-FIT01', 'Z-FIT02')
  AND stat_date >= CURDATE();

DELETE FROM monitor_metric_daily
WHERE metric_code = 'INFLOW_TOTAL_DAILY_AVG'
  AND stat_date >= CURDATE();

DROP PROCEDURE IF EXISTS sp_refresh_all_metrics;
DROP PROCEDURE IF EXISTS sp_refresh_inflow_totalizer_metric;

CREATE OR REPLACE VIEW vw_inflow_totalizer_latest AS
SELECT total_value AS totalizer_sum_value,
       collect_time AS latest_collect_time
FROM monitor_totalizer_snapshot
WHERE point_code = 'INFLOW_TOTALIZER'
ORDER BY collect_time DESC
LIMIT 1;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_assert_monitor_point_rules$$
CREATE PROCEDURE sp_assert_monitor_point_rules()
BEGIN
  DECLARE v_enabled INT;
  DECLARE v_minute INT;
  DECLARE v_hour INT;
  SELECT COUNT(*),
         SUM(raw_save_cycle_type = 'minute'),
         SUM(raw_save_cycle_type = 'hour')
    INTO v_enabled, v_minute, v_hour
  FROM monitor_point
  WHERE enabled = 1;
  IF v_enabled <> 37 OR v_minute <> 2 OR v_hour <> 35 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'monitor point migration failed: expected 37 enabled, 2 minute, 35 hour points';
  END IF;
END$$
CALL sp_assert_monitor_point_rules()$$
DROP PROCEDURE sp_assert_monitor_point_rules$$
DELIMITER ;

INSERT INTO monitor_schema_migration (migration_code)
VALUES ('20260712_monitor_persistence_rules');

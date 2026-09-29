-- 2026-07-13 monitoring storage v2
-- Cumulative meter readings are independent from instantaneous flow averages.
-- All persisted numeric measurements use three decimal places.

CREATE TABLE IF NOT EXISTS monitor_schema_migration (
  migration_code VARCHAR(128) PRIMARY KEY,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_guard_monitor_storage_v2$$
CREATE PROCEDURE sp_guard_monitor_storage_v2()
BEGIN
  IF EXISTS (
    SELECT 1 FROM monitor_schema_migration
    WHERE migration_code = '20260713_monitor_storage_v2'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'migration 20260713_monitor_storage_v2 is already applied';
  END IF;
END$$
CALL sp_guard_monitor_storage_v2()$$
DROP PROCEDURE sp_guard_monitor_storage_v2$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_refresh_inflow_totalizer_metric;
DROP VIEW IF EXISTS vw_inflow_totalizer_latest;

DROP TABLE IF EXISTS monitor_metric_realtime;
DROP TABLE IF EXISTS monitor_metric_realtime_backup_20260712;
DROP TABLE IF EXISTS monitor_totalizer_snapshot_backup_20260712;
DROP TABLE IF EXISTS monitor_aggregate_rule_member;
DROP TABLE IF EXISTS monitor_aggregate_rule;

-- The old table contained software-integrated increments. The new table stores
-- the direct sum of the two PLC cumulative meters, so old rows are not reused.
DROP TABLE IF EXISTS monitor_totalizer_snapshot;

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

CREATE TABLE monitor_totalizer_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  collect_time DATETIME NOT NULL,
  total_value DECIMAL(20,3) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_totalizer_time (collect_time)
);

ALTER TABLE monitor_point_data
  MODIFY value_decimal DECIMAL(18,3) NULL;

DELETE older
FROM monitor_point_data older
JOIN monitor_point_data newer
  ON newer.point_code = older.point_code
 AND newer.collect_time = older.collect_time
 AND newer.id < older.id;

SET @point_minute_index_sql = IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'monitor_point_data'
      AND index_name = 'uk_point_code_time'
  ),
  'SELECT 1',
  'ALTER TABLE monitor_point_data ADD UNIQUE KEY uk_point_code_time (point_code, collect_time)'
);
PREPARE point_minute_index_stmt FROM @point_minute_index_sql;
EXECUTE point_minute_index_stmt;
DEALLOCATE PREPARE point_minute_index_stmt;

ALTER TABLE monitor_point_hourly
  MODIFY avg_value DECIMAL(18,3) NULL,
  MODIFY min_value DECIMAL(18,3) NULL,
  MODIFY max_value DECIMAL(18,3) NULL;

ALTER TABLE monitor_point_daily
  MODIFY avg_value DECIMAL(18,3) NULL,
  MODIFY min_value DECIMAL(18,3) NULL,
  MODIFY max_value DECIMAL(18,3) NULL;

ALTER TABLE monitor_metric_hourly
  MODIFY value_decimal DECIMAL(18,3) NOT NULL;

ALTER TABLE monitor_metric_daily
  MODIFY value_decimal DECIMAL(18,3) NOT NULL;

DELETE FROM monitor_point_hourly
WHERE point_code IN ('Z-FIT01', 'Z-FIT02');

DELETE FROM monitor_point_daily
WHERE point_code IN ('Z-FIT01', 'Z-FIT02');

DELETE FROM monitor_metric_hourly
WHERE metric_code NOT IN ('Z-FIT01', 'Z-FIT02');

DELETE FROM monitor_metric_daily
WHERE metric_code NOT IN ('Z-FIT01', 'Z-FIT02');

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

INSERT INTO monitor_schema_migration (migration_code)
VALUES ('20260713_monitor_storage_v2')
ON DUPLICATE KEY UPDATE applied_at = applied_at;

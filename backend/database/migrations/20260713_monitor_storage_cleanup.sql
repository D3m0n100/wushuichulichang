-- 2026-07-13 monitoring storage cleanup
-- 1. monitor_totalizer_snapshot is the only source of cumulative inflow data.
-- 2. Z-FIT01/Z-FIT02 hourly and daily averages are stored separately in metric tables.
-- 3. monitor_point_daily is calculated from monitor_point_hourly and excludes Z-FIT01/Z-FIT02.

CREATE TABLE IF NOT EXISTS monitor_schema_migration (
  migration_code VARCHAR(128) PRIMARY KEY,
  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP PROCEDURE IF EXISTS sp_refresh_all_metrics;
DROP PROCEDURE IF EXISTS sp_refresh_inflow_totalizer_metric;
DROP PROCEDURE IF EXISTS sp_refresh_inflow_hourly_metric;
DROP PROCEDURE IF EXISTS sp_refresh_inflow_daily_metric;
DROP PROCEDURE IF EXISTS sp_refresh_point_hourly;
DROP PROCEDURE IF EXISTS sp_refresh_point_daily;

DELETE FROM monitor_metric_hourly
WHERE metric_code = 'INFLOW_TOTAL_HOURLY_AVG';

DELETE FROM monitor_metric_daily
WHERE metric_code = 'INFLOW_TOTAL_DAILY_AVG';

DELETE FROM monitor_point_hourly
WHERE point_code IN ('Z-FIT01', 'Z-FIT02');

DELETE FROM monitor_point_daily
WHERE point_code IN ('Z-FIT01', 'Z-FIT02');

INSERT INTO monitor_metric_hourly
  (metric_code, metric_name, stat_hour, value_decimal, sample_count)
SELECT d.point_code,
       CASE d.point_code
         WHEN 'Z-FIT01' THEN 'phase1 inflow hourly average'
         ELSE 'phase2 inflow hourly average'
       END,
       STR_TO_DATE(DATE_FORMAT(d.collect_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s'),
       AVG(d.value_decimal),
       COUNT(*)
FROM monitor_point_data d
WHERE d.point_code IN ('Z-FIT01', 'Z-FIT02')
  AND d.value_decimal IS NOT NULL
GROUP BY d.point_code,
         STR_TO_DATE(DATE_FORMAT(d.collect_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s')
ON DUPLICATE KEY UPDATE
  metric_name = VALUES(metric_name),
  value_decimal = VALUES(value_decimal),
  sample_count = VALUES(sample_count);

INSERT INTO monitor_metric_daily
  (metric_code, metric_name, stat_date, value_decimal, sample_count)
SELECT d.point_code,
       CASE d.point_code
         WHEN 'Z-FIT01' THEN 'phase1 inflow daily average'
         ELSE 'phase2 inflow daily average'
       END,
       DATE(d.collect_time),
       AVG(d.value_decimal),
       COUNT(*)
FROM monitor_point_data d
WHERE d.point_code IN ('Z-FIT01', 'Z-FIT02')
  AND d.value_decimal IS NOT NULL
GROUP BY d.point_code, DATE(d.collect_time)
ON DUPLICATE KEY UPDATE
  metric_name = VALUES(metric_name),
  value_decimal = VALUES(value_decimal),
  sample_count = VALUES(sample_count);

-- Rebuild the other points' daily values exclusively from their hourly rows.
DELETE FROM monitor_point_daily;
INSERT INTO monitor_point_daily
  (point_id, point_code, stat_date, avg_value, min_value, max_value, sample_count)
SELECT h.point_id,
       h.point_code,
       DATE(h.stat_hour),
       AVG(h.avg_value),
       MIN(h.min_value),
       MAX(h.max_value),
       SUM(h.sample_count)
FROM monitor_point_hourly h
WHERE h.point_code NOT IN ('Z-FIT01', 'Z-FIT02')
  AND h.avg_value IS NOT NULL
GROUP BY h.point_id, h.point_code, DATE(h.stat_hour);

DROP TABLE IF EXISTS monitor_metric_realtime;
DROP TABLE IF EXISTS monitor_metric_realtime_backup_20260712;
DROP TABLE IF EXISTS monitor_totalizer_snapshot_backup_20260712;

CREATE OR REPLACE VIEW vw_inflow_hourly_total AS
SELECT stat_hour,
       SUM(value_decimal) AS total_hour_avg_value,
       SUM(sample_count) AS sample_count
FROM monitor_metric_hourly
WHERE metric_code IN ('Z-FIT01', 'Z-FIT02')
GROUP BY stat_hour;

CREATE OR REPLACE VIEW vw_inflow_daily_total AS
SELECT stat_date,
       SUM(value_decimal) AS total_day_avg_value,
       SUM(sample_count) AS sample_count
FROM monitor_metric_daily
WHERE metric_code IN ('Z-FIT01', 'Z-FIT02')
GROUP BY stat_date;

CREATE OR REPLACE VIEW vw_inflow_totalizer_latest AS
SELECT total_value AS totalizer_sum_value,
       collect_time AS latest_collect_time
FROM monitor_totalizer_snapshot
WHERE point_code = 'INFLOW_TOTALIZER'
ORDER BY collect_time DESC
LIMIT 1;

INSERT INTO monitor_schema_migration (migration_code)
VALUES ('20260713_monitor_storage_cleanup')
ON DUPLICATE KEY UPDATE applied_at = applied_at;

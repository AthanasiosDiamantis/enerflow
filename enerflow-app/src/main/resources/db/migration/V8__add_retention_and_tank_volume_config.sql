-- V8__add_retention_and_tank_volume_config.sql
-- Adds configuration keys needed for AP3.2: tank volume (for the kWh
-- calculation in control_log) and snapshot retention period (automatic
-- data purging per US-05-01). A new migration is used because already-applied
-- migrations (like V2) must never be edited.

INSERT INTO device_config (config_key, config_value, description) VALUES
        ('hotwater.tank.volume.liters', '300', 'Hot water tank volume in liters, used for the kWh calculation in control_log'),
        ('data.retention.snapshot.days', '730', 'Number of days heatpump_snapshot and battery_snapshot rows are kept before automatic deletion (~2 years)');
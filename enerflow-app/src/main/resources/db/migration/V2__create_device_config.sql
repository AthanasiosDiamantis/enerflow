-- Migration V2: Create device_config table
-- Stores runtime-configurable thresholds and setpoints for EnerFlow automation.
-- Values can be updated at runtime without application restart.

CREATE TABLE device_config (
   config_key      VARCHAR(100) PRIMARY KEY,
   config_value    VARCHAR(255) NOT NULL,
   description     VARCHAR(500),
   last_updated    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Default values for EnerFlow automation
INSERT INTO device_config (config_key, config_value, description) VALUES
  ('pv.surplus.threshold.watts',        '300',  'Minimum PV surplus in watts required to trigger hot water boost'),
  ('battery.soc.threshold.percent',     '90',   'Minimum battery state of charge (USOC) in percent required to trigger hot water boost'),
  ('hotwater.setpoint.elevated.celsius','55',   'Hot water setpoint in Celsius when PV surplus boost is active'),
  ('hotwater.setpoint.normal.celsius',  '48',   'Hot water setpoint in Celsius when boost is inactive (restore value)');
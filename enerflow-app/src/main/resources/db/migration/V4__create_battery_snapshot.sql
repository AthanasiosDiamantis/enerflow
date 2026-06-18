-- V4__create_battery_snapshot.sql
-- Stores polling snapshots from the sonnenBatterie every 60 seconds (mirrors heatpump_snapshot)

CREATE TABLE battery_snapshot (
                                  id                      BIGSERIAL PRIMARY KEY,
                                  timestamp               TIMESTAMP NOT NULL,
                                  usable_soc_percent      INTEGER NOT NULL,
                                  production_watts        INTEGER NOT NULL,
                                  consumption_watts       INTEGER NOT NULL,
                                  battery_power_watts     INTEGER NOT NULL,
                                  battery_charging        BOOLEAN NOT NULL,
                                  battery_discharging     BOOLEAN NOT NULL,
                                  surplus_watts           INTEGER NOT NULL
);

CREATE INDEX idx_battery_snapshot_timestamp
    ON battery_snapshot (timestamp DESC);
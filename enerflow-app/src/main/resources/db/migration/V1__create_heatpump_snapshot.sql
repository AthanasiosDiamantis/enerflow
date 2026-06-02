-- Migration V1: Create heatpump_snapshot table
-- Stores polling snapshots from the Novelan heat pump every 60 seconds

CREATE TABLE heatpump_snapshot (
                                   id                  BIGSERIAL PRIMARY KEY,
                                   timestamp           TIMESTAMP NOT NULL,
                                   vorlauf_temp        DOUBLE PRECISION,
                                   ruecklauf_temp      DOUBLE PRECISION,
                                   warmwasser_ist      DOUBLE PRECISION,
                                   warmwasser_soll     DOUBLE PRECISION,
                                   aussentemperatur    DOUBLE PRECISION,
                                   heizleistung_kw     DOUBLE PRECISION,
                                   leistungsaufnahme_kw DOUBLE PRECISION,
                                   betriebszustand     VARCHAR(100),
                                   betriebsstunden_wp  INTEGER,
                                   hysterese_ww_k      DOUBLE PRECISION
);

CREATE INDEX idx_heatpump_snapshot_timestamp
    ON heatpump_snapshot (timestamp DESC);
-- V5__create_control_log.sql
-- Append-only audit log for every setpoint change EnerFlow performs (RAISE/RESET)
-- or a user performs manually (MANUAL). Rows are never updated or deleted.

CREATE TABLE control_log (
                             id                      BIGSERIAL PRIMARY KEY,
                             timestamp               TIMESTAMP NOT NULL,
                             action                  VARCHAR(20) NOT NULL,
                             old_setpoint_celsius    DOUBLE PRECISION,
                             new_setpoint_celsius    DOUBLE PRECISION,
                             berechnung_kwh          DOUBLE PRECISION,
                             triggered_by            VARCHAR(100),
                             CONSTRAINT chk_control_log_action CHECK (action IN ('RAISE', 'RESET', 'MANUAL'))
    );

CREATE INDEX idx_control_log_timestamp
    ON control_log (timestamp DESC);
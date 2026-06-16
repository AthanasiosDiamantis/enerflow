-- Migration V3: Create enerflow_state table
-- Stores the runtime state of the EnerFlow automation logic.
-- Exactly one row exists at all times (singleton state, id = 1).

CREATE TABLE enerflow_state (
                                id                      INTEGER PRIMARY KEY DEFAULT 1,
                                enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
                                boost_active            BOOLEAN NOT NULL DEFAULT FALSE,
                                last_known_setpoint     DOUBLE PRECISION,
                                last_updated            TIMESTAMP NOT NULL DEFAULT NOW(),
                                CONSTRAINT chk_singleton CHECK (id = 1)
);

-- Initialize the single state row
INSERT INTO enerflow_state (id, enabled, boost_active, last_known_setpoint)
VALUES (1, TRUE, FALSE, NULL);
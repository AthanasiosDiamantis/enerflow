-- Stores the user-configured electricity price used for savings calculations.
-- One active row at all times (id = 1), updated in place.
-- Change history is tracked in electricity_price_change_log.

CREATE TABLE electricity_price_config (
                                          id            INTEGER      NOT NULL DEFAULT 1,
                                          price_ct_kwh  NUMERIC(6,2) NOT NULL DEFAULT 28.00,
                                          updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                                          updated_by    VARCHAR(100) NOT NULL DEFAULT 'system',
                                          CONSTRAINT pk_electricity_price_config PRIMARY KEY (id),
                                          CONSTRAINT chk_price_positive CHECK (price_ct_kwh > 0)
);

-- Seed the default value so the application always finds a row
INSERT INTO electricity_price_config (id, price_ct_kwh, updated_at, updated_by)
VALUES (1, 28.00, NOW(), 'system');

-- Append-only change log for auditing price changes (US-01-04)
CREATE TABLE electricity_price_change_log (
                                              id           BIGSERIAL    NOT NULL,
                                              changed_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
                                              old_price    NUMERIC(6,2) NOT NULL,
                                              new_price    NUMERIC(6,2) NOT NULL,
                                              changed_by   VARCHAR(100) NOT NULL,
                                              CONSTRAINT pk_electricity_price_change_log PRIMARY KEY (id)
);
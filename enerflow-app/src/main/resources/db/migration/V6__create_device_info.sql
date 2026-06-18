-- V6__create_device_info.sql
-- Read-only hardware metadata reported by the connected devices themselves
-- (serial number, firmware version, IP). Distinct from device_config, which
-- holds user-configurable parameters.

CREATE TABLE device_info (
                             device_type        VARCHAR(50) PRIMARY KEY,
                             serial_number       VARCHAR(100),
                             firmware_version    VARCHAR(100),
                             ip_address          VARCHAR(45),
                             last_updated        TIMESTAMP NOT NULL DEFAULT NOW()
);
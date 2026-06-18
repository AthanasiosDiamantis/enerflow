-- V7__create_app_user.sql
-- User accounts for EnerFlow's role-based access control (full login logic follows in AP3.3).
-- Passwords are BCrypt-hashed; plaintext is never persisted.

CREATE TABLE app_user (
                          id                      BIGSERIAL PRIMARY KEY,
                          username                VARCHAR(100) NOT NULL UNIQUE,
                          password_hash           VARCHAR(255) NOT NULL,
                          role                    VARCHAR(20) NOT NULL,
                          enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
                          failed_login_attempts   INTEGER NOT NULL DEFAULT 0,
                          locked_until            TIMESTAMP,
                          created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
                          last_login              TIMESTAMP,
                          CONSTRAINT chk_app_user_role CHECK (role IN ('ROLE_USER', 'ROLE_MANAGER', 'ROLE_ADMIN'))
);
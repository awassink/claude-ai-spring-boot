CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE drivers (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    license_number      VARCHAR(50)  NOT NULL,
    phone_number        VARCHAR(20),
    email               VARCHAR(150) NOT NULL,
    license_expiry_date DATE         NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ,

    CONSTRAINT pk_drivers PRIMARY KEY (id),
    CONSTRAINT uq_drivers_license_number UNIQUE (license_number),
    CONSTRAINT uq_drivers_email UNIQUE (email)
);

CREATE INDEX idx_driver_license_number ON drivers (license_number);
CREATE INDEX idx_driver_email ON drivers (email);

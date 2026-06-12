--liquibase formatted sql

--changeset triptrekker:1781254344-pgcrypto splitStatements:true endDelimiter:;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

--changeset triptrekker:1781254344-uuidv7 splitStatements:false
CREATE OR REPLACE FUNCTION uuidv7()
RETURNS UUID
AS $$
DECLARE
    unix_ts_ms BYTEA;
    rand_a BYTEA;
    rand_b BYTEA;
BEGIN
    unix_ts_ms := substring(int8send((EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT) FROM 3);
    rand_a := gen_random_bytes(2);
    rand_b := gen_random_bytes(8);

    RETURN encode(
        unix_ts_ms ||
        set_byte(rand_a, 0, (get_byte(rand_a, 0) & 15) | 112) ||
        set_byte(rand_b, 0, (get_byte(rand_b, 0) & 63) | 128),
        'hex'
    )::UUID;
END;
$$ LANGUAGE plpgsql VOLATILE;

--changeset triptrekker:1781254344-initial-schema splitStatements:true endDelimiter:;
-- ======================================================
-- USER / CUSTOMER TABLES
-- ======================================================

CREATE TABLE user_type (
    user_type_id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_type_name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE app_user (
    user_id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_type_id UUID NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_email VARCHAR(320) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_app_user_user_type_id
        FOREIGN KEY (user_type_id) REFERENCES user_type(user_type_id)
);

CREATE TABLE role (
    role_id UUID PRIMARY KEY DEFAULT uuidv7(),
    role_name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user_id
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role_id
        FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE
);

CREATE TABLE address (
    address_id UUID PRIMARY KEY DEFAULT uuidv7(),
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120),
    zip VARCHAR(40),
    country VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE customer (
    customer_id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL UNIQUE,
    address_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_customer_user_id
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_address_id
        FOREIGN KEY (address_id) REFERENCES address(address_id) ON DELETE SET NULL
);

CREATE TABLE customer_preferences (
    customer_id UUID PRIMARY KEY,
    preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_customer_preferences_customer_id
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE
);

-- ======================================================
-- LOOKUPS
-- ======================================================

CREATE TABLE booking_status (
    booking_status_id UUID PRIMARY KEY DEFAULT uuidv7(),
    status VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE booking_type (
    booking_type_id UUID PRIMARY KEY DEFAULT uuidv7(),
    code VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE payment_integration_type (
    payment_integration_type_id UUID PRIMARY KEY DEFAULT uuidv7(),
    type VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE payment_status (
    payment_status_id UUID PRIMARY KEY DEFAULT uuidv7(),
    status VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE notification_type (
    notification_type_id UUID PRIMARY KEY DEFAULT uuidv7(),
    type VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE notification_status (
    notification_status_id UUID PRIMARY KEY DEFAULT uuidv7(),
    status VARCHAR(60) NOT NULL UNIQUE
);

INSERT INTO booking_status (status)
VALUES ('PENDING'), ('RESERVED'), ('CONFIRMED'), ('CANCELLED'), ('EXPIRED')
ON CONFLICT (status) DO NOTHING;

INSERT INTO booking_type (code)
VALUES ('FLIGHT'), ('HOTEL')
ON CONFLICT (code) DO NOTHING;

INSERT INTO payment_status (status)
VALUES ('PENDING'), ('AUTHORIZED'), ('CAPTURED'), ('FAILED'), ('REFUNDED'), ('CANCELLED')
ON CONFLICT (status) DO NOTHING;

INSERT INTO payment_integration_type (type)
VALUES ('STRIPE'), ('PAYPAL'), ('MANUAL')
ON CONFLICT (type) DO NOTHING;

INSERT INTO notification_type (type)
VALUES ('EMAIL'), ('SMS')
ON CONFLICT (type) DO NOTHING;

INSERT INTO notification_status (status)
VALUES ('CREATED'), ('SENT'), ('NOT_SENT'), ('FAILED')
ON CONFLICT (status) DO NOTHING;

-- ======================================================
-- PAYMENT / TRANSACTION / BOOKING TABLES
-- ======================================================

CREATE TABLE payment_type_configuration (
    payment_type_configuration_id UUID PRIMARY KEY DEFAULT uuidv7(),
    payment_integration_type_id UUID NOT NULL,
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_payment_type_configuration_payment_integration_type_id
        FOREIGN KEY (payment_integration_type_id)
        REFERENCES payment_integration_type(payment_integration_type_id)
);

CREATE TABLE preferred_payment_setting (
    customer_id UUID PRIMARY KEY,
    payment_integration_type_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_preferred_payment_setting_customer_id
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_preferred_payment_setting_payment_integration_type_id
        FOREIGN KEY (payment_integration_type_id)
        REFERENCES payment_integration_type(payment_integration_type_id)
);

CREATE TABLE transaction (
    transaction_id UUID PRIMARY KEY DEFAULT uuidv7(),
    payment_integration_type_id UUID NOT NULL,
    payment_status_id UUID NOT NULL,
    external_order_id VARCHAR(255),
    amount NUMERIC(12, 2) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_transaction_payment_integration_type_id
        FOREIGN KEY (payment_integration_type_id)
        REFERENCES payment_integration_type(payment_integration_type_id),
    CONSTRAINT fk_transaction_payment_status_id
        FOREIGN KEY (payment_status_id) REFERENCES payment_status(payment_status_id)
);

CREATE TABLE transaction_details (
    transaction_id UUID PRIMARY KEY,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_transaction_details_transaction_id
        FOREIGN KEY (transaction_id) REFERENCES transaction(transaction_id) ON DELETE CASCADE
);

CREATE TABLE payment (
    payment_id UUID PRIMARY KEY DEFAULT uuidv7(),
    transaction_id UUID NOT NULL,
    payment_integration_type_id UUID NOT NULL,
    payment_status_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    external_payment_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_payment_transaction_id
        FOREIGN KEY (transaction_id) REFERENCES transaction(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_payment_integration_type_id
        FOREIGN KEY (payment_integration_type_id)
        REFERENCES payment_integration_type(payment_integration_type_id),
    CONSTRAINT fk_payment_payment_status_id
        FOREIGN KEY (payment_status_id) REFERENCES payment_status(payment_status_id)
);

CREATE TABLE booking (
    booking_id UUID PRIMARY KEY DEFAULT uuidv7(),
    booking_type_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    booking_status_id UUID NOT NULL,
    transaction_id UUID,
    booking_code VARCHAR(120) NOT NULL UNIQUE,
    fees NUMERIC(12, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_booking_booking_type_id
        FOREIGN KEY (booking_type_id) REFERENCES booking_type(booking_type_id),
    CONSTRAINT fk_booking_customer_id
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
    CONSTRAINT fk_booking_booking_status_id
        FOREIGN KEY (booking_status_id) REFERENCES booking_status(booking_status_id),
    CONSTRAINT fk_booking_transaction_id
        FOREIGN KEY (transaction_id) REFERENCES transaction(transaction_id)
);

CREATE TABLE flight_booking (
    flight_booking_id UUID PRIMARY KEY DEFAULT uuidv7(),
    booking_id UUID NOT NULL UNIQUE,
    provider VARCHAR(80),
    provider_offer_request_id VARCHAR(255),
    provider_offer_id VARCHAR(255),
    provider_order_id VARCHAR(255),
    offer_expires_at TIMESTAMPTZ,
    total_amount NUMERIC(12, 2),
    base_amount NUMERIC(12, 2),
    tax_amount NUMERIC(12, 2),
    currency_code CHAR(3),
    airline_name VARCHAR(255),
    airline_iata_code VARCHAR(10),
    refundable BOOLEAN,
    changeable BOOLEAN,
    origin VARCHAR(20),
    destination VARCHAR(20),
    departure_at TIMESTAMPTZ,
    return_at TIMESTAMPTZ,
    departure_local_at TIMESTAMP,
    departure_timezone VARCHAR(80),
    arrival_local_at TIMESTAMP,
    arrival_timezone VARCHAR(80),
    trip_type VARCHAR(40),
    provider_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_flight_booking_booking_id
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE
);

CREATE TABLE hotel_booking (
    hotel_booking_id UUID PRIMARY KEY DEFAULT uuidv7(),
    booking_id UUID NOT NULL UNIQUE,
    provider VARCHAR(80),
    provider_search_id VARCHAR(255),
    provider_offer_id VARCHAR(255),
    provider_reservation_id VARCHAR(255),
    provider_hotel_id VARCHAR(255),
    hotel_name VARCHAR(255),
    check_in_date DATE,
    check_out_date DATE,
    city VARCHAR(120),
    country VARCHAR(120),
    offer_expires_at TIMESTAMPTZ,
    total_amount NUMERIC(12, 2),
    base_amount NUMERIC(12, 2),
    tax_amount NUMERIC(12, 2),
    currency_code CHAR(3),
    room_name VARCHAR(255),
    rate_plan_name VARCHAR(255),
    refundable BOOLEAN,
    cancellation_policy JSONB,
    provider_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_hotel_booking_booking_id
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE
);

-- ======================================================
-- NOTIFICATION / AUDIT TABLES
-- ======================================================

CREATE TABLE notification (
    notification_id UUID PRIMARY KEY DEFAULT uuidv7(),
    customer_id UUID NOT NULL,
    booking_id UUID,
    notification_type_id UUID NOT NULL,
    notification_status_id UUID NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_notification_customer_id
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
    CONSTRAINT fk_notification_booking_id
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_notification_type_id
        FOREIGN KEY (notification_type_id) REFERENCES notification_type(notification_type_id),
    CONSTRAINT fk_notification_notification_status_id
        FOREIGN KEY (notification_status_id) REFERENCES notification_status(notification_status_id)
);

CREATE TABLE auditing (
    auditing_id UUID PRIMARY KEY DEFAULT uuidv7(),
    correlation_id UUID,
    actor_id VARCHAR(255),
    actor_type VARCHAR(20),
    entity_type VARCHAR(120),
    entity_id VARCHAR(255),
    action VARCHAR(80),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE SEQUENCE revinfo_rev_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE revinfo (
    rev BIGINT PRIMARY KEY DEFAULT nextval('revinfo_rev_seq'),
    rev_tstmp TIMESTAMPTZ NOT NULL,
    actor_id VARCHAR(255),
    actor_type VARCHAR(20),
    correlation_id UUID
);

CREATE TABLE integration_audit_log (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    correlation_id UUID,
    actor_id VARCHAR(255),
    actor_type VARCHAR(20),
    vendor VARCHAR(50) NOT NULL,
    api_endpoint VARCHAR(500) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    http_status INTEGER,
    request_payload TEXT,
    response_payload TEXT,
    duration_ms BIGINT,
    success BOOLEAN NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

-- Hibernate Envers shadow tables for audited payment, transaction, and booking entities.
CREATE TABLE payment_aud (
    payment_id UUID NOT NULL,
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    transaction_id UUID,
    payment_integration_type_id UUID,
    payment_status_id UUID,
    amount NUMERIC(12, 2),
    currency_code CHAR(3),
    external_payment_id VARCHAR(255),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    version INTEGER,
    PRIMARY KEY (payment_id, rev),
    CONSTRAINT fk_payment_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE transaction_aud (
    transaction_id UUID NOT NULL,
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    payment_integration_type_id UUID,
    payment_status_id UUID,
    external_order_id VARCHAR(255),
    amount NUMERIC(12, 2),
    currency_code CHAR(3),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    version INTEGER,
    PRIMARY KEY (transaction_id, rev),
    CONSTRAINT fk_transaction_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE booking_aud (
    booking_id UUID NOT NULL,
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    booking_type_id UUID,
    customer_id UUID,
    booking_status_id UUID,
    transaction_id UUID,
    booking_code VARCHAR(120),
    fees NUMERIC(12, 2),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    version INTEGER,
    PRIMARY KEY (booking_id, rev),
    CONSTRAINT fk_booking_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE flight_booking_aud (
    flight_booking_id UUID NOT NULL,
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    booking_id UUID,
    provider VARCHAR(80),
    provider_offer_request_id VARCHAR(255),
    provider_offer_id VARCHAR(255),
    provider_order_id VARCHAR(255),
    offer_expires_at TIMESTAMPTZ,
    total_amount NUMERIC(12, 2),
    base_amount NUMERIC(12, 2),
    tax_amount NUMERIC(12, 2),
    currency_code CHAR(3),
    airline_name VARCHAR(255),
    airline_iata_code VARCHAR(10),
    refundable BOOLEAN,
    changeable BOOLEAN,
    origin VARCHAR(20),
    destination VARCHAR(20),
    departure_at TIMESTAMPTZ,
    return_at TIMESTAMPTZ,
    departure_local_at TIMESTAMP,
    departure_timezone VARCHAR(80),
    arrival_local_at TIMESTAMP,
    arrival_timezone VARCHAR(80),
    trip_type VARCHAR(40),
    provider_payload JSONB,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    version INTEGER,
    PRIMARY KEY (flight_booking_id, rev),
    CONSTRAINT fk_flight_booking_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE hotel_booking_aud (
    hotel_booking_id UUID NOT NULL,
    rev BIGINT NOT NULL,
    revtype SMALLINT,
    booking_id UUID,
    provider VARCHAR(80),
    provider_search_id VARCHAR(255),
    provider_offer_id VARCHAR(255),
    provider_reservation_id VARCHAR(255),
    provider_hotel_id VARCHAR(255),
    hotel_name VARCHAR(255),
    check_in_date DATE,
    check_out_date DATE,
    city VARCHAR(120),
    country VARCHAR(120),
    offer_expires_at TIMESTAMPTZ,
    total_amount NUMERIC(12, 2),
    base_amount NUMERIC(12, 2),
    tax_amount NUMERIC(12, 2),
    currency_code CHAR(3),
    room_name VARCHAR(255),
    rate_plan_name VARCHAR(255),
    refundable BOOLEAN,
    cancellation_policy JSONB,
    provider_payload JSONB,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    version INTEGER,
    PRIMARY KEY (hotel_booking_id, rev),
    CONSTRAINT fk_hotel_booking_aud_rev
        FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE INDEX idx_app_user_email ON app_user(user_email);
CREATE INDEX idx_booking_customer_id ON booking(customer_id);
CREATE INDEX idx_booking_booking_status_id ON booking(booking_status_id);
CREATE INDEX idx_booking_transaction_id ON booking(transaction_id);
CREATE INDEX idx_payment_transaction_id ON payment(transaction_id);
CREATE INDEX idx_notification_customer_id ON notification(customer_id);
CREATE INDEX idx_notification_booking_id ON notification(booking_id);
CREATE INDEX idx_notification_notification_status_id ON notification(notification_status_id);
CREATE INDEX idx_revinfo_rev_tstmp ON revinfo(rev_tstmp);
CREATE INDEX idx_integration_audit_log_correlation_id ON integration_audit_log(correlation_id);
CREATE INDEX idx_integration_audit_log_vendor_success ON integration_audit_log(vendor, success);
CREATE INDEX idx_integration_audit_log_occurred_at ON integration_audit_log(occurred_at);

COMMENT ON COLUMN app_user.user_type_id IS 'Coarse user category such as customer or admin; authorization permissions belong in role/user_role.';
COMMENT ON TABLE role IS 'Application authorization roles. Keep distinct from user_type, which is a coarse account category.';
COMMENT ON TABLE user_role IS 'Many-to-many assignment of authorization roles to users.';
COMMENT ON TABLE app_user IS 'Application user profile. Authentication credentials are intentionally external to this table.';
COMMENT ON TABLE auditing IS 'Structured custom audit log for business events that are not covered by Hibernate Envers revision tables or integration_audit_log.';

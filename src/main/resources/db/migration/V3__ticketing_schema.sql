CREATE TABLE events (
    id  BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    venue VARCHAR(200) NOT NULL,
    currency VARCHAR(3)  NOT NULL DEFAULT 'KES',
    starts_at TIMESTAMPTZ   NOT NULL,
    sales_open_at  TIMESTAMPTZ   NOT NULL,
    sales_close_at TIMESTAMPTZ   NOT NULL,
    status   VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT events_sales_window_chk CHECK (sales_close_at > sales_open_at)
);

CREATE TABLE ticket_tiers (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    price_cents  BIGINT  NOT NULL,
    total_quantity  INTEGER   NOT NULL,
    available_quantity INTEGER NOT NULL,
    CONSTRAINT ticket_tiers_price_chk     CHECK (price_cents >= 0),
   CONSTRAINT ticket_tiers_inventory_chk CHECK (available_quantity >= 0 AND available_quantity <= total_quantity),
   CONSTRAINT ticket_tiers_event_name_uq UNIQUE (event_id, name)
);

CREATE TABLE reservations (
    id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id  BIGINT  NOT NULL REFERENCES users (id),
    event_id   BIGINT   NOT NULL REFERENCES events (id),
    status VARCHAR(20)  NOT NULL,
    total_cents  BIGINT  NOT NULL,
    currency  VARCHAR(3)   NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash  VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    confirmed_at  TIMESTAMPTZ,
    CONSTRAINT reservations_idem_uq UNIQUE (user_id, idempotency_key)
);


CREATE TABLE reservation_items (
    id BIGSERIAL PRIMARY KEY,
    reservation_id   UUID    NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    tier_id  BIGINT  NOT NULL REFERENCES ticket_tiers (id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_cents BIGINT  NOT NULL,
    CONSTRAINT reservation_items_price_chk CHECK (unit_price_cents >= 0),
    CONSTRAINT reservation_items_uq UNIQUE (reservation_id, tier_id)
);

CREATE TABLE payments (
    id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id   UUID  NOT NULL REFERENCES reservations (id),
    user_id  BIGINT  NOT NULL REFERENCES users (id),
    provider VARCHAR(30)  NOT NULL,
    provider_ref   VARCHAR(100),
    amount_cents   BIGINT   NOT NULL,
    currency  VARCHAR(3)   NOT NULL,
    status  VARCHAR(20)  NOT NULL,
    idempotency_key  VARCHAR(100) NOT NULL,
    failure_reason   VARCHAR(255),
    refund_attempts  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT payments_idem_uq UNIQUE (user_id, idempotency_key),
    CONSTRAINT payments_amount_chk CHECK (amount_cents >= 0)
);


CREATE TABLE payment_webhook_events (
    id  BIGSERIAL PRIMARY KEY,
    provider    VARCHAR(30)  NOT NULL,
    event_id    VARCHAR(100) NOT NULL,
    received_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT payment_webhook_events_uq UNIQUE (provider, event_id)
);

CREATE TABLE tickets (
    id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL REFERENCES reservations (id),
    user_id BIGINT   NOT NULL REFERENCES users (id),
    event_id BIGINT   NOT NULL REFERENCES events (id),
    tier_id  BIGINT  NOT NULL REFERENCES ticket_tiers (id),
    seq  INTEGER     NOT NULL,
    code  VARCHAR(64) NOT NULL UNIQUE,
    status  VARCHAR(20) NOT NULL,
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked_in_at  TIMESTAMPTZ,
    CONSTRAINT tickets_issue_uq UNIQUE (reservation_id, tier_id, seq)
);

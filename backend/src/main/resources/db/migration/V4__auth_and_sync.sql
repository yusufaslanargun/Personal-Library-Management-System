CREATE TABLE app_user (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login_at TIMESTAMPTZ
);

ALTER TABLE list
  ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE list_item
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE progress_log
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE loan
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE external_link
  ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE sync_state
  ADD COLUMN client_id VARCHAR(64),
  ADD COLUMN needs_full_sync BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE sync_outbox (
  id BIGSERIAL PRIMARY KEY,
  entity_type VARCHAR(40) NOT NULL,
  entity_key VARCHAR(200) NOT NULL,
  operation VARCHAR(12) NOT NULL,
  queued_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sync_outbox_queued_at ON sync_outbox(queued_at);

CREATE TABLE sync_remote_store (
  id INTEGER PRIMARY KEY,
  payload JSONB NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE media_item (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(10) NOT NULL,
  title TEXT NOT NULL,
  year INT NOT NULL,
  condition TEXT,
  location TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  deleted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  progress_percent INT NOT NULL DEFAULT 0,
  progress_value INT NOT NULL DEFAULT 0,
  total_value INT NOT NULL DEFAULT 0,
  search_vector TSVECTOR
);

CREATE TABLE book_info (
  item_id BIGINT PRIMARY KEY REFERENCES media_item(id) ON DELETE CASCADE,
  isbn VARCHAR(20),
  pages INT,
  publisher TEXT,
  authors_text TEXT
);

CREATE TABLE book_author (
  item_id BIGINT NOT NULL REFERENCES book_info(item_id) ON DELETE CASCADE,
  author TEXT NOT NULL
);

CREATE TABLE dvd_info (
  item_id BIGINT PRIMARY KEY REFERENCES media_item(id) ON DELETE CASCADE,
  runtime INT,
  director TEXT
);

CREATE TABLE dvd_cast (
  item_id BIGINT NOT NULL REFERENCES dvd_info(item_id) ON DELETE CASCADE,
  member TEXT NOT NULL
);

CREATE TABLE external_link (
  id BIGSERIAL PRIMARY KEY,
  item_id BIGINT NOT NULL REFERENCES media_item(id) ON DELETE CASCADE,
  provider VARCHAR(40) NOT NULL,
  external_id TEXT,
  url TEXT,
  rating NUMERIC(3,1),
  summary TEXT,
  last_sync_at TIMESTAMPTZ
);

CREATE TABLE tag (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE media_item_tag (
  item_id BIGINT NOT NULL REFERENCES media_item(id) ON DELETE CASCADE,
  tag_id BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
  PRIMARY KEY (item_id, tag_id)
);

CREATE TABLE list (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE list_item (
  list_id BIGINT NOT NULL REFERENCES list(id) ON DELETE CASCADE,
  item_id BIGINT NOT NULL REFERENCES media_item(id) ON DELETE CASCADE,
  position INT NOT NULL,
  priority INT NOT NULL DEFAULT 0,
  PRIMARY KEY (list_id, item_id)
);

CREATE TABLE progress_log (
  id BIGSERIAL PRIMARY KEY,
  item_id BIGINT NOT NULL REFERENCES media_item(id) ON DELETE CASCADE,
  log_date DATE NOT NULL,
  duration_minutes INT,
  page_or_minute INT NOT NULL,
  percent INT NOT NULL
);

CREATE TABLE loan (
  id BIGSERIAL PRIMARY KEY,
  item_id BIGINT NOT NULL REFERENCES media_item(id) ON DELETE CASCADE,
  to_whom TEXT NOT NULL,
  start_date DATE NOT NULL,
  due_date DATE NOT NULL,
  returned_at DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT chk_loan_due CHECK (due_date >= start_date)
);

CREATE UNIQUE INDEX uniq_active_loan_per_item ON loan(item_id) WHERE returned_at IS NULL;

CREATE TABLE sync_state (
  id INT PRIMARY KEY,
  last_sync_at TIMESTAMPTZ,
  last_status TEXT,
  last_conflict_count INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_media_item_status ON media_item(status);
CREATE INDEX idx_media_item_type ON media_item(type);
CREATE INDEX idx_media_item_year ON media_item(year);
CREATE INDEX idx_media_item_deleted_at ON media_item(deleted_at);
CREATE INDEX idx_list_item_list_position ON list_item(list_id, position);
CREATE INDEX idx_progress_item ON progress_log(item_id);
CREATE INDEX idx_loan_item ON loan(item_id);
CREATE INDEX idx_external_item ON external_link(item_id);

CREATE OR REPLACE FUNCTION update_media_item_search_vector(mi_id BIGINT) RETURNS VOID AS $$
BEGIN
  UPDATE media_item
  SET search_vector = to_tsvector('simple',
    coalesce(title, '') || ' ' ||
    coalesce(location, '') || ' ' ||
    coalesce(condition, '') || ' ' ||
    coalesce((SELECT authors_text FROM book_info WHERE item_id = mi_id), '') || ' ' ||
    coalesce((SELECT publisher FROM book_info WHERE item_id = mi_id), '') || ' ' ||
    coalesce((SELECT director FROM dvd_info WHERE item_id = mi_id), '')
  )
  WHERE id = mi_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION media_item_search_trigger() RETURNS TRIGGER AS $$
BEGIN
  PERFORM update_media_item_search_vector(NEW.id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER media_item_search_update
AFTER INSERT OR UPDATE ON media_item
FOR EACH ROW EXECUTE FUNCTION media_item_search_trigger();

CREATE OR REPLACE FUNCTION book_info_search_trigger() RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    PERFORM update_media_item_search_vector(OLD.item_id);
    RETURN OLD;
  END IF;
  PERFORM update_media_item_search_vector(NEW.item_id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER book_info_search_update
AFTER INSERT OR UPDATE OR DELETE ON book_info
FOR EACH ROW EXECUTE FUNCTION book_info_search_trigger();

CREATE OR REPLACE FUNCTION dvd_info_search_trigger() RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    PERFORM update_media_item_search_vector(OLD.item_id);
    RETURN OLD;
  END IF;
  PERFORM update_media_item_search_vector(NEW.item_id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER dvd_info_search_update
AFTER INSERT OR UPDATE OR DELETE ON dvd_info
FOR EACH ROW EXECUTE FUNCTION dvd_info_search_trigger();

CREATE INDEX idx_media_item_search_vector ON media_item USING GIN (search_vector);

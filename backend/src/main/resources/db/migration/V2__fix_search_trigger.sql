DROP TRIGGER IF EXISTS media_item_search_update ON media_item;
DROP TRIGGER IF EXISTS book_info_search_update ON book_info;
DROP TRIGGER IF EXISTS dvd_info_search_update ON dvd_info;

DROP FUNCTION IF EXISTS media_item_search_trigger();
DROP FUNCTION IF EXISTS book_info_search_trigger();
DROP FUNCTION IF EXISTS dvd_info_search_trigger();
DROP FUNCTION IF EXISTS update_media_item_search_vector(BIGINT);

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
  NEW.search_vector = to_tsvector('simple',
    coalesce(NEW.title, '') || ' ' ||
    coalesce(NEW.location, '') || ' ' ||
    coalesce(NEW.condition, '') || ' ' ||
    coalesce((SELECT authors_text FROM book_info WHERE item_id = NEW.id), '') || ' ' ||
    coalesce((SELECT publisher FROM book_info WHERE item_id = NEW.id), '') || ' ' ||
    coalesce((SELECT director FROM dvd_info WHERE item_id = NEW.id), '')
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER media_item_search_update
BEFORE INSERT OR UPDATE ON media_item
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

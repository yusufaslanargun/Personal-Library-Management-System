ALTER TABLE external_link
  ADD CONSTRAINT uq_external_link_provider_external_id UNIQUE (provider, external_id);

DROP TRIGGER IF EXISTS dvd_cast_search_update ON dvd_cast;
DROP FUNCTION IF EXISTS dvd_cast_search_trigger();

CREATE OR REPLACE FUNCTION update_media_item_search_vector(mi_id BIGINT) RETURNS VOID AS $$
BEGIN
  UPDATE media_item
  SET search_vector = to_tsvector('simple',
    coalesce(title, '') || ' ' ||
    coalesce(location, '') || ' ' ||
    coalesce(condition, '') || ' ' ||
    coalesce((SELECT authors_text FROM book_info WHERE item_id = mi_id), '') || ' ' ||
    coalesce((SELECT publisher FROM book_info WHERE item_id = mi_id), '') || ' ' ||
    coalesce((SELECT director FROM dvd_info WHERE item_id = mi_id), '') || ' ' ||
    coalesce((SELECT string_agg(member, ' ') FROM dvd_cast WHERE item_id = mi_id), '')
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
    coalesce((SELECT director FROM dvd_info WHERE item_id = NEW.id), '') || ' ' ||
    coalesce((SELECT string_agg(member, ' ') FROM dvd_cast WHERE item_id = NEW.id), '')
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION dvd_cast_search_trigger() RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    PERFORM update_media_item_search_vector(OLD.item_id);
    RETURN OLD;
  END IF;
  PERFORM update_media_item_search_vector(NEW.item_id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER dvd_cast_search_update
AFTER INSERT OR UPDATE OR DELETE ON dvd_cast
FOR EACH ROW EXECUTE FUNCTION dvd_cast_search_trigger();

SELECT update_media_item_search_vector(id) FROM media_item;

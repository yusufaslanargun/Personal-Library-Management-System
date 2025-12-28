INSERT INTO media_item (type, title, year, condition, location, status, created_at, updated_at,
  progress_percent, progress_value, total_value)
SELECT
  CASE WHEN gs % 2 = 0 THEN 'BOOK' ELSE 'DVD' END,
  'Seed Item ' || gs,
  1990 + (gs % 30),
  'Good',
  'Shelf ' || (gs % 10),
  'AVAILABLE',
  now(),
  now(),
  0,
  0,
  CASE WHEN gs % 2 = 0 THEN 300 ELSE 120 END
FROM generate_series(1, 5000) AS gs;

INSERT INTO book_info (item_id, isbn, pages, publisher, authors_text)
SELECT id,
  'ISBN' || id,
  300,
  'Publisher ' || (id % 20),
  'Author ' || (id % 50)
FROM media_item
WHERE type = 'BOOK';

INSERT INTO book_author (item_id, author)
SELECT id, 'Author ' || (id % 50)
FROM media_item
WHERE type = 'BOOK';

INSERT INTO dvd_info (item_id, runtime, director)
SELECT id,
  120,
  'Director ' || (id % 30)
FROM media_item
WHERE type = 'DVD';

INSERT INTO dvd_cast (item_id, member)
SELECT id,
  'Actor ' || (id % 40)
FROM media_item
WHERE type = 'DVD';

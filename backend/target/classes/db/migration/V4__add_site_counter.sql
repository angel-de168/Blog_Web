CREATE TABLE IF NOT EXISTS site_counter (
  name VARCHAR(64) PRIMARY KEY,
  value BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL
);

INSERT INTO site_counter (name, value, updated_at)
SELECT 'home_visit', 0, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM site_counter WHERE name = 'home_visit'
);

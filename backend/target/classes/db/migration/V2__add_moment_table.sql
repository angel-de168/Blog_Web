CREATE TABLE IF NOT EXISTS moment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  mood VARCHAR(32) DEFAULT '',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_moment_user_updated (user_id, updated_at),
  CONSTRAINT fk_moment_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);


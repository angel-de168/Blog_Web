ALTER TABLE note
  ADD COLUMN cover_image VARCHAR(255) DEFAULT NULL,
  ADD COLUMN read_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS note_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  note_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_note_comment_note_created (note_id, created_at),
  CONSTRAINT fk_note_comment_note FOREIGN KEY (note_id) REFERENCES note(id),
  CONSTRAINT fk_note_comment_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS moment_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  moment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_moment_like_user (moment_id, user_id),
  INDEX idx_moment_like_moment (moment_id),
  CONSTRAINT fk_moment_like_moment FOREIGN KEY (moment_id) REFERENCES moment(id),
  CONSTRAINT fk_moment_like_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS moment_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  moment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_moment_comment_moment_created (moment_id, created_at),
  CONSTRAINT fk_moment_comment_moment FOREIGN KEY (moment_id) REFERENCES moment(id),
  CONSTRAINT fk_moment_comment_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

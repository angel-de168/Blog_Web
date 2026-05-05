CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  follower_user_id BIGINT NOT NULL,
  following_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_follower_following (follower_user_id, following_user_id),
  INDEX idx_follower_created (follower_user_id, created_at),
  INDEX idx_following_created (following_user_id, created_at),
  CONSTRAINT fk_user_follow_follower FOREIGN KEY (follower_user_id) REFERENCES app_user(id),
  CONSTRAINT fk_user_follow_following FOREIGN KEY (following_user_id) REFERENCES app_user(id)
);

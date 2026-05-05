CREATE TABLE IF NOT EXISTS private_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_a_id BIGINT NOT NULL,
  user_b_id BIGINT NOT NULL,
  last_message_id BIGINT NULL,
  last_message_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_user_pair (user_a_id, user_b_id),
  INDEX idx_user_a_last_time (user_a_id, last_message_at),
  INDEX idx_user_b_last_time (user_b_id, last_message_at),
  CONSTRAINT fk_private_session_user_a FOREIGN KEY (user_a_id) REFERENCES app_user(id),
  CONSTRAINT fk_private_session_user_b FOREIGN KEY (user_b_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS private_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_session_created (session_id, created_at),
  CONSTRAINT fk_private_message_session FOREIGN KEY (session_id) REFERENCES private_session(id),
  CONSTRAINT fk_private_message_sender FOREIGN KEY (sender_user_id) REFERENCES app_user(id)
);

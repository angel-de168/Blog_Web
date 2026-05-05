ALTER TABLE moment
  ADD COLUMN visibility_scope VARCHAR(24) NOT NULL DEFAULT 'PUBLIC';

UPDATE moment
SET visibility_scope = 'PUBLIC'
WHERE visibility_scope IS NULL OR visibility_scope = '';

CREATE INDEX idx_moment_user_scope_updated ON moment (user_id, visibility_scope, updated_at);

CREATE TABLE beta_feedback (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category VARCHAR(20) NOT NULL CHECK (category IN ('BUG', 'SUGGESTION', 'OTHER')),
  description VARCHAR(2000) NOT NULL,
  path VARCHAR(300),
  request_id VARCHAR(80),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_beta_feedback_user_created ON beta_feedback(user_id, created_at DESC);

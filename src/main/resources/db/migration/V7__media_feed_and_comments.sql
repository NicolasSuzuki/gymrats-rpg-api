ALTER TABLE workout_sessions ADD COLUMN xp_awarded INTEGER;
ALTER TABLE workout_sessions
  ADD CONSTRAINT ck_workout_sessions_xp CHECK (
    (status = 'IN_PROGRESS' AND xp_awarded IS NULL)
    OR (status = 'COMPLETED' AND xp_awarded >= 0)
    OR (status = 'CANCELLED' AND xp_awarded = 0)
  );

CREATE TABLE workout_media (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  session_id UUID NOT NULL UNIQUE REFERENCES workout_sessions(id) ON DELETE CASCADE,
  storage_key VARCHAR(160) NOT NULL UNIQUE,
  original_file_name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(40) NOT NULL CHECK (mime_type IN ('image/jpeg', 'image/png', 'image/webp')),
  size_bytes BIGINT NOT NULL CHECK (size_bytes BETWEEN 1 AND 5242880),
  sha256 VARCHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE feed_posts (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  workout_session_id UUID NOT NULL UNIQUE REFERENCES workout_sessions(id) ON DELETE CASCADE,
  media_id UUID REFERENCES workout_media(id) ON DELETE SET NULL,
  category VARCHAR(24) NOT NULL CHECK (category IN ('WORKOUT')),
  title VARCHAR(160) NOT NULL,
  caption VARCHAR(280),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE feed_comments (
  id UUID PRIMARY KEY,
  post_id UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content VARCHAR(500) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE feed_publish_operations (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  idempotency_key VARCHAR(120) NOT NULL,
  session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
  post_id UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (owner_id, idempotency_key)
);

CREATE INDEX idx_feed_posts_created ON feed_posts(created_at DESC);
CREATE INDEX idx_feed_posts_owner_created ON feed_posts(owner_id, created_at DESC);
CREATE INDEX idx_feed_comments_post_created ON feed_comments(post_id, created_at);

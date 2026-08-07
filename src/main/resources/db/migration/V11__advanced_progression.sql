CREATE TABLE class_mastery_events (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  character_class VARCHAR(20) NOT NULL
    CHECK (character_class IN ('FIGHTER', 'TANK', 'ASSASSIN', 'RANGER', 'HEALER', 'MAGE')),
  source_id UUID NOT NULL,
  amount INTEGER NOT NULL CHECK (amount > 0),
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (user_id, character_class, source_id)
);

CREATE TABLE advanced_rank_profiles (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  current_rank VARCHAR(16) NOT NULL
    CHECK (current_rank IN ('A', 'S5', 'S4', 'S3', 'S2', 'S1', 'NATIONAL')),
  s1_since TIMESTAMP WITH TIME ZONE,
  national_since TIMESTAMP WITH TIME ZONE,
  pause_started_at TIMESTAMP WITH TIME ZONE,
  pause_reason VARCHAR(160),
  s1_paused_seconds BIGINT NOT NULL DEFAULT 0 CHECK (s1_paused_seconds >= 0),
  protected_until TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE advanced_rank_events (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  previous_rank VARCHAR(16) NOT NULL
    CHECK (previous_rank IN ('A', 'S5', 'S4', 'S3', 'S2', 'S1', 'NATIONAL')),
  new_rank VARCHAR(16) NOT NULL
    CHECK (new_rank IN ('A', 'S5', 'S4', 'S3', 'S2', 'S1', 'NATIONAL')),
  reason VARCHAR(160) NOT NULL,
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_class_mastery_user_occurred
  ON class_mastery_events(user_id, occurred_at);
CREATE INDEX idx_advanced_rank_events_user_occurred
  ON advanced_rank_events(user_id, occurred_at);

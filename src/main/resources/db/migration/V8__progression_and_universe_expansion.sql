ALTER TABLE users DROP CONSTRAINT ck_users_universe;
ALTER TABLE users
  ADD CONSTRAINT ck_users_universe CHECK (universe IN ('HUNTER', 'FITNESS', 'SPOTLIGHT', 'VIGIL'));
ALTER TABLE users ADD COLUMN selected_title VARCHAR(80);
ALTER TABLE users ADD COLUMN ranking_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE xp_events (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  source_type VARCHAR(24) NOT NULL CHECK (source_type IN ('WORKOUT_CHECK_IN')),
  source_id UUID NOT NULL,
  amount INTEGER NOT NULL CHECK (amount > 0),
  occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (user_id, source_type, source_id)
);

CREATE TABLE user_achievements (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  code VARCHAR(40) NOT NULL,
  unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (user_id, code)
);

CREATE INDEX idx_xp_events_user_occurred ON xp_events(user_id, occurred_at);
CREATE INDEX idx_user_achievements_user ON user_achievements(user_id, unlocked_at);

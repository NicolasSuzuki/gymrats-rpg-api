ALTER TABLE users ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN character_class VARCHAR(20);
ALTER TABLE users ADD COLUMN universe VARCHAR(20) NOT NULL DEFAULT 'HUNTER';

ALTER TABLE users
  ADD CONSTRAINT ck_users_character_class CHECK (
    character_class IS NULL OR character_class IN ('FIGHTER', 'TANK', 'ASSASSIN', 'RANGER', 'HEALER', 'MAGE')
  );
ALTER TABLE users
  ADD CONSTRAINT ck_users_universe CHECK (universe IN ('HUNTER', 'FITNESS'));

CREATE TABLE user_modalities (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  modality VARCHAR(20) NOT NULL CHECK (modality IN ('ACADEMIA', 'CORRIDA', 'CAMINHADA', 'NATACAO')),
  PRIMARY KEY (user_id, modality)
);

CREATE TABLE weekly_goal_versions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target INTEGER NOT NULL CHECK (target BETWEEN 1 AND 7),
  current_user_id UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
  valid_until TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK ((valid_until IS NULL AND current_user_id = user_id)
    OR (valid_until IS NOT NULL AND current_user_id IS NULL)),
  CHECK (valid_until IS NULL OR valid_until >= valid_from)
);

CREATE INDEX idx_weekly_goal_history
  ON weekly_goal_versions(user_id, valid_from);

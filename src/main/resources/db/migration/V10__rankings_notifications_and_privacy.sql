CREATE TABLE notifications (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_id VARCHAR(160) NOT NULL,
  kind VARCHAR(24) NOT NULL CHECK (
    kind IN ('COMMENT', 'FRIENDSHIP', 'GROUP', 'ACHIEVEMENT', 'PROGRESSION', 'RETURN')
  ),
  title VARCHAR(120) NOT NULL,
  message VARCHAR(300) NOT NULL,
  href VARCHAR(240),
  read_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (user_id, event_id)
);

CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read_at);

CREATE TABLE notification_preferences (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  kind VARCHAR(24) NOT NULL CHECK (
    kind IN ('COMMENT', 'FRIENDSHIP', 'GROUP', 'ACHIEVEMENT', 'PROGRESSION', 'RETURN')
  ),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (user_id, kind)
);

CREATE TABLE ranking_snapshots (
  id UUID PRIMARY KEY,
  snapshot_key VARCHAR(180) NOT NULL,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  scope VARCHAR(16) NOT NULL CHECK (scope IN ('GLOBAL', 'GROUP')),
  group_id UUID REFERENCES social_groups(id) ON DELETE CASCADE,
  period VARCHAR(16) NOT NULL CHECK (period IN ('WEEKLY', 'MONTHLY')),
  metric VARCHAR(20) NOT NULL CHECK (metric IN ('XP', 'CONSISTENCY')),
  period_start TIMESTAMP WITH TIME ZONE NOT NULL,
  period_end TIMESTAMP WITH TIME ZONE NOT NULL,
  position INTEGER NOT NULL CHECK (position > 0),
  score_value INTEGER NOT NULL CHECK (score_value >= 0),
  distinct_days INTEGER NOT NULL CHECK (distinct_days >= 0),
  check_ins INTEGER NOT NULL CHECK (check_ins >= 0),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (snapshot_key, user_id)
);

CREATE INDEX idx_ranking_snapshots_period
  ON ranking_snapshots(scope, group_id, period, metric, period_start);

CREATE TABLE monarch_reigns (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  ended_at TIMESTAMP WITH TIME ZONE,
  active_slot SMALLINT UNIQUE CHECK (active_slot = 1),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK (ended_at IS NULL OR ended_at >= started_at),
  CHECK (
    (ended_at IS NULL AND active_slot = 1)
    OR (ended_at IS NOT NULL AND active_slot IS NULL)
  )
);

CREATE TABLE monarch_state (
  singleton_id SMALLINT PRIMARY KEY CHECK (singleton_id = 1),
  current_reign_id UUID UNIQUE REFERENCES monarch_reigns(id),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO monarch_state (singleton_id, current_reign_id, updated_at)
VALUES (1, NULL, CURRENT_TIMESTAMP);

CREATE INDEX idx_monarch_reigns_user_started
  ON monarch_reigns(user_id, started_at);

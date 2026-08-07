CREATE TABLE workout_sessions (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  active_user_id UUID UNIQUE,
  status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
  source VARCHAR(20) NOT NULL CHECK (source IN ('PLAN', 'FREE')),
  plan_version_id UUID REFERENCES workout_plans(id),
  started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK ((status = 'IN_PROGRESS' AND active_user_id = owner_id AND completed_at IS NULL)
    OR (status <> 'IN_PROGRESS' AND active_user_id IS NULL AND completed_at IS NOT NULL)),
  CHECK ((source = 'PLAN' AND plan_version_id IS NOT NULL) OR source = 'FREE')
);

CREATE TABLE workout_session_exercises (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
  exercise_id UUID NOT NULL,
  position INTEGER NOT NULL CHECK (position >= 0),
  status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
  notes VARCHAR(1000),
  UNIQUE (session_id, exercise_id),
  UNIQUE (session_id, position)
);

CREATE TABLE workout_session_sets (
  id UUID PRIMARY KEY,
  session_exercise_id UUID NOT NULL REFERENCES workout_session_exercises(id) ON DELETE CASCADE,
  position INTEGER NOT NULL CHECK (position >= 0),
  load_kg DECIMAL(8,2) CHECK (load_kg IS NULL OR load_kg >= 0),
  repetitions INTEGER CHECK (repetitions IS NULL OR repetitions >= 0),
  duration_seconds INTEGER CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
  distance_meters INTEGER CHECK (distance_meters IS NULL OR distance_meters >= 0),
  rest_seconds INTEGER CHECK (rest_seconds IS NULL OR rest_seconds >= 0),
  rpe INTEGER CHECK (rpe IS NULL OR rpe BETWEEN 1 AND 10),
  UNIQUE (session_exercise_id, position)
);

CREATE TABLE workout_idempotency_operations (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  idempotency_key VARCHAR(120) NOT NULL,
  operation VARCHAR(20) NOT NULL CHECK (operation IN ('COMPLETE', 'CANCEL')),
  session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (owner_id, idempotency_key)
);

CREATE INDEX idx_workout_sessions_owner_status ON workout_sessions(owner_id, status);

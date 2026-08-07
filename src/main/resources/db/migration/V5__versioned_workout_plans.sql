CREATE TABLE workout_plans (
  id UUID PRIMARY KEY,
  root_id UUID NOT NULL,
  current_root_id UUID UNIQUE,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(120) NOT NULL,
  modality VARCHAR(20) NOT NULL CHECK (modality IN ('ACADEMIA', 'CORRIDA', 'CAMINHADA', 'NATACAO')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
  version INTEGER NOT NULL CHECK (version > 0),
  current_version BOOLEAN NOT NULL,
  previous_version_id UUID REFERENCES workout_plans(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK ((current_version = TRUE AND current_root_id = root_id)
    OR (current_version = FALSE AND current_root_id IS NULL)),
  UNIQUE (root_id, version)
);

CREATE TABLE workout_plan_exercises (
  id UUID PRIMARY KEY,
  plan_id UUID NOT NULL REFERENCES workout_plans(id) ON DELETE CASCADE,
  exercise_id UUID NOT NULL REFERENCES exercises(id),
  position INTEGER NOT NULL CHECK (position >= 0),
  sets INTEGER CHECK (sets IS NULL OR sets > 0),
  repetitions INTEGER CHECK (repetitions IS NULL OR repetitions > 0),
  duration_minutes INTEGER CHECK (duration_minutes IS NULL OR duration_minutes > 0),
  distance_meters INTEGER CHECK (distance_meters IS NULL OR distance_meters > 0),
  UNIQUE (plan_id, position),
  UNIQUE (plan_id, exercise_id)
);

CREATE INDEX idx_workout_plans_owner ON workout_plans(owner_id, current_version, status);

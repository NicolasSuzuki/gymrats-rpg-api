ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN terms_version VARCHAR(40);
ALTER TABLE users ADD COLUMN photo_storage_consent BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_workout_sessions_owner_completed ON workout_sessions(owner_id, status, completed_at DESC);
CREATE INDEX idx_session_exercises_history ON workout_session_exercises(exercise_id, status, session_id);

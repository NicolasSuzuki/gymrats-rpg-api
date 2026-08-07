CREATE TABLE exercises (
  id UUID PRIMARY KEY,
  owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(120) NOT NULL,
  modality VARCHAR(20) NOT NULL CHECK (modality IN ('ACADEMIA', 'CORRIDA', 'CAMINHADA', 'NATACAO')),
  category VARCHAR(80) NOT NULL,
  muscle_group VARCHAR(80),
  description VARCHAR(1000),
  equipment VARCHAR(120),
  difficulty VARCHAR(20) CHECK (difficulty IS NULL OR difficulty IN ('INICIANTE', 'INTERMEDIARIO', 'AVANCADO')),
  terrain VARCHAR(80),
  stroke VARCHAR(80),
  pool_length_meters INTEGER CHECK (pool_length_meters IS NULL OR pool_length_meters BETWEEN 10 AND 100),
  visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('GLOBAL', 'PERSONAL')),
  archived BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK ((visibility = 'GLOBAL' AND owner_id IS NULL) OR (visibility = 'PERSONAL' AND owner_id IS NOT NULL))
);

CREATE TABLE exercise_instructions (
  exercise_id UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
  position INTEGER NOT NULL,
  instruction VARCHAR(500) NOT NULL,
  PRIMARY KEY (exercise_id, position)
);

CREATE INDEX idx_exercises_search ON exercises(modality, category, archived);
CREATE INDEX idx_exercises_owner ON exercises(owner_id, archived);

INSERT INTO exercises (id, name, modality, category, muscle_group, equipment, difficulty, visibility, archived, created_at, updated_at) VALUES
  ('10000000-0000-0000-0000-000000000001', 'Agachamento livre', 'ACADEMIA', 'Força', 'Pernas', 'Barra', 'INTERMEDIARIO', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000002', 'Supino reto', 'ACADEMIA', 'Força', 'Peitoral', 'Barra', 'INTERMEDIARIO', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000003', 'Remada curvada', 'ACADEMIA', 'Força', 'Costas', 'Barra', 'INTERMEDIARIO', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000004', 'Desenvolvimento', 'ACADEMIA', 'Força', 'Ombros', 'Halteres', 'INICIANTE', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000005', 'Corrida leve', 'CORRIDA', 'Cardio', NULL, NULL, 'INICIANTE', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000006', 'Tiros intervalados', 'CORRIDA', 'Velocidade', NULL, NULL, 'AVANCADO', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000007', 'Caminhada ao ar livre', 'CAMINHADA', 'Cardio', NULL, NULL, 'INICIANTE', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('10000000-0000-0000-0000-000000000008', 'Nado livre', 'NATACAO', 'Cardio', NULL, NULL, 'INTERMEDIARIO', 'GLOBAL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

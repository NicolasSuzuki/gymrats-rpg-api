CREATE TABLE duo_partnerships (
  id UUID PRIMARY KEY,
  status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE','ENDED')),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  ended_at TIMESTAMP WITH TIME ZONE
);
CREATE TABLE duo_members (
  duo_id UUID NOT NULL REFERENCES duo_partnerships(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  active_user_id UUID UNIQUE,
  joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
  reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  encouragement_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY(duo_id,user_id),
  CHECK (active_user_id IS NULL OR active_user_id = user_id)
);
CREATE TABLE duo_invites (
  token VARCHAR(96) PRIMARY KEY,
  issuer_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  duo_id UUID REFERENCES duo_partnerships(id) ON DELETE CASCADE
);
CREATE TABLE duo_meetings (
  id UUID PRIMARY KEY,
  duo_id UUID NOT NULL REFERENCES duo_partnerships(id) ON DELETE CASCADE,
  proposer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
  location VARCHAR(120),
  note VARCHAR(280),
  status VARCHAR(12) NOT NULL CHECK (status IN ('PENDING','ACCEPTED','DECLINED')),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE(duo_id,proposer_id,scheduled_at)
);
CREATE TABLE duo_private_media (
  id UUID PRIMARY KEY,
  duo_id UUID NOT NULL REFERENCES duo_partnerships(id) ON DELETE CASCADE,
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  storage_key VARCHAR(255) NOT NULL UNIQUE,
  mime_type VARCHAR(40) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE duo_memories (
  id UUID PRIMARY KEY,
  duo_id UUID NOT NULL REFERENCES duo_partnerships(id) ON DELETE CASCADE,
  author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
  caption VARCHAR(280),
  media_id UUID UNIQUE REFERENCES duo_private_media(id) ON DELETE SET NULL,
  idempotency_key VARCHAR(120) NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE(duo_id,session_id),
  UNIQUE(duo_id,author_id,idempotency_key)
);
CREATE TABLE duo_reactions (
  memory_id UUID NOT NULL REFERENCES duo_memories(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type VARCHAR(16) NOT NULL CHECK (type IN ('PROUD','TOGETHER','YOU_CAN')),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY(memory_id,user_id,type)
);
CREATE TABLE motivation_preferences (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  title_id VARCHAR(80) NOT NULL DEFAULT '',
  frame_id VARCHAR(40) NOT NULL DEFAULT 'none',
  item_id VARCHAR(40) NOT NULL DEFAULT 'none'
);
CREATE INDEX idx_duo_memories_recent ON duo_memories(duo_id,created_at DESC);
CREATE INDEX idx_duo_meetings_upcoming ON duo_meetings(duo_id,scheduled_at);
CREATE INDEX idx_duo_media_membership ON duo_private_media(duo_id);

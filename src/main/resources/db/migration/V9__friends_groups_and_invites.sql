CREATE TABLE friend_requests (
  id UUID PRIMARY KEY,
  sender_id UUID NOT NULL REFERENCES users(id),
  receiver_id UUID NOT NULL REFERENCES users(id),
  status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CHECK (sender_id <> receiver_id)
);

CREATE INDEX idx_friend_requests_receiver ON friend_requests(receiver_id, status, created_at);
CREATE INDEX idx_friend_requests_sender ON friend_requests(sender_id, status, created_at);

CREATE TABLE friendships (
  id UUID PRIMARY KEY,
  user_low_id UUID NOT NULL REFERENCES users(id),
  user_high_id UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (user_low_id, user_high_id),
  CHECK (user_low_id <> user_high_id)
);

CREATE INDEX idx_friendships_low ON friendships(user_low_id);
CREATE INDEX idx_friendships_high ON friendships(user_high_id);

CREATE TABLE social_groups (
  id UUID PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(300),
  visibility VARCHAR(16) NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
  admin_id UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE group_members (
  id UUID PRIMARY KEY,
  group_id UUID NOT NULL REFERENCES social_groups(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id),
  role VARCHAR(16) NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
  joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE (group_id, user_id)
);

CREATE INDEX idx_group_members_user ON group_members(user_id);

CREATE TABLE group_invites (
  id UUID PRIMARY KEY,
  group_id UUID NOT NULL REFERENCES social_groups(id) ON DELETE CASCADE,
  created_by UUID NOT NULL REFERENCES users(id),
  token VARCHAR(80) NOT NULL UNIQUE,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at TIMESTAMP WITH TIME ZONE,
  used_by UUID REFERENCES users(id),
  used_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_group_invites_group ON group_invites(group_id, expires_at);

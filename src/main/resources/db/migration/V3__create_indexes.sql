-- For refresh token user lookups
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- For joining user_email to users and emails
CREATE INDEX idx_user_email_user_id ON user_email (user_id);
CREATE INDEX idx_user_email_email_id ON user_email (email_id);

-- For joining user_device to users and devices
CREATE INDEX idx_user_device_user_id ON user_device (user_id);
CREATE INDEX idx_user_device_device_id ON user_device (device_id);

-- Prevents linking the same active email to a user twice & speeds up user email queries
CREATE UNIQUE INDEX idx_user_email_active_user_email
    ON user_email (user_id, email_id)
    WHERE deleted_at IS NULL;

-- Speeds up looking up active user accounts by email_id
CREATE INDEX idx_user_email_active_email
    ON user_email (email_id)
    WHERE deleted_at IS NULL;

-- Prevents linking the same active device to a user twice & speeds up user device queries
CREATE UNIQUE INDEX idx_user_device_active_user_device
    ON user_device (user_id, device_id)
    WHERE deleted_at IS NULL;

-- Speeds up looking up active users/roles associated with a specific device
CREATE INDEX idx_user_device_active_device
    ON user_device (device_id)
    WHERE deleted_at IS NULL;

-- Speeds up verifying active, non-expired tokens during authentication
CREATE INDEX idx_refresh_tokens_user_expires
    ON refresh_tokens (user_id, expires_at);

-- Speeds up background cleanup jobs (e.g., DELETE FROM refresh_tokens WHERE expires_at < NOW())
CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

-- 为 user_points 添加赠予积分与过期时间
ALTER TABLE user_points
    ADD COLUMN IF NOT EXISTS gift_points DECIMAL(18, 6) NOT NULL DEFAULT 0;

ALTER TABLE user_points
    ADD COLUMN IF NOT EXISTS gift_expire_time TIMESTAMP NULL;


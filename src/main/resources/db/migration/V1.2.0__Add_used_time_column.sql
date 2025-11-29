-- 保障 points_key 存在 used_time 列，避免排序时报列不存在
ALTER TABLE "points_key"
    ADD COLUMN IF NOT EXISTS "used_time" TIMESTAMP;


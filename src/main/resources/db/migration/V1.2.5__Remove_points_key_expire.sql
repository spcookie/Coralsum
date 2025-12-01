-- 删除密钥表的过期时间列
ALTER TABLE points_key
    DROP COLUMN expire_time;

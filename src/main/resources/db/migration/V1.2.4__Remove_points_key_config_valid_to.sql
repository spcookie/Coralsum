-- 删除积分密钥配置表的结束时间列
ALTER TABLE points_key_config
    DROP COLUMN valid_to;

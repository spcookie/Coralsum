-- 保障新方案所需结构存在
ALTER TABLE outlet_user
    ADD COLUMN IF NOT EXISTS nick_tag INT;
CREATE UNIQUE INDEX IF NOT EXISTS uq_outlet_user_name_tag ON outlet_user (user_source, nick_name, nick_tag);

CREATE TABLE IF NOT EXISTS nick_name_bitmap
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_source VARCHAR(32)     NOT NULL,
    base_name   VARCHAR(255)    NOT NULL,
    bitmap      VARBINARY(1250) NOT NULL,
    version     BIGINT,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_nick_bitmap ON nick_name_bitmap (user_source, base_name);

-- 兼容旧方案：移除已过时表
DROP TABLE IF EXISTS nick_name_pool;
DROP TABLE IF EXISTS nick_numbers;

-- 可选：初始化位图行（零位图由应用层写入时创建，无需预插入）
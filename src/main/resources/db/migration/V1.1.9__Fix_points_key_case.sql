-- 修正 H2 大小写与引号不一致导致的查询失败：
-- 将未引用创建的表（默认为大写）重命名为带引号的小写，并重命名列为带引号的小写。

-- 修正 points_key 表名与列名
ALTER TABLE POINTS_KEY
    RENAME TO "points_key";

ALTER TABLE "points_key"
    ALTER COLUMN ID RENAME TO "id";
ALTER TABLE "points_key"
    ALTER COLUMN KEY_CODE RENAME TO "key_code";
ALTER TABLE "points_key"
    ALTER COLUMN CONFIG_ID RENAME TO "config_id";
ALTER TABLE "points_key"
    ALTER COLUMN CONFIG_JSON RENAME TO "config_json";
ALTER TABLE "points_key"
    ALTER COLUMN USED RENAME TO "used";
ALTER TABLE "points_key"
    ALTER COLUMN USED_TIME RENAME TO "used_time";
ALTER TABLE "points_key"
    ALTER COLUMN USED_IP RENAME TO "used_ip";
ALTER TABLE "points_key"
    ALTER COLUMN USED_UID RENAME TO "used_uid";
ALTER TABLE "points_key"
    ALTER COLUMN ENABLED RENAME TO "enabled";
ALTER TABLE "points_key"
    ALTER COLUMN EXPIRE_TIME RENAME TO "expire_time";
ALTER TABLE "points_key"
    ALTER COLUMN CREATE_TIME RENAME TO "create_time";
ALTER TABLE "points_key"
    ALTER COLUMN UPDATE_TIME RENAME TO "update_time";
ALTER TABLE "points_key"
    ALTER COLUMN CREATE_BY RENAME TO "create_by";
ALTER TABLE "points_key"
    ALTER COLUMN UPDATE_BY RENAME TO "update_by";

-- 修正 points_key_config 表名与列名
ALTER TABLE POINTS_KEY_CONFIG
    RENAME TO "points_key_config";

ALTER TABLE "points_key_config"
    ALTER COLUMN ID RENAME TO "id";
ALTER TABLE "points_key_config"
    ALTER COLUMN NAME RENAME TO "name";
ALTER TABLE "points_key_config"
    ALTER COLUMN PERMANENT_POINTS RENAME TO "permanent_points";
ALTER TABLE "points_key_config"
    ALTER COLUMN SUBSCRIBE_POINTS RENAME TO "subscribe_points";
ALTER TABLE "points_key_config"
    ALTER COLUMN SUBSCRIBE_TYPE RENAME TO "subscribe_type";
ALTER TABLE "points_key_config"
    ALTER COLUMN SUBSCRIBE_DURATION_DAYS RENAME TO "subscribe_duration_days";
ALTER TABLE "points_key_config"
    ALTER COLUMN VALID_FROM RENAME TO "valid_from";
ALTER TABLE "points_key_config"
    ALTER COLUMN VALID_TO RENAME TO "valid_to";
ALTER TABLE "points_key_config"
    ALTER COLUMN DISABLED RENAME TO "disabled";
ALTER TABLE "points_key_config"
    ALTER COLUMN CREATE_TIME RENAME TO "create_time";
ALTER TABLE "points_key_config"
    ALTER COLUMN UPDATE_TIME RENAME TO "update_time";
ALTER TABLE "points_key_config"
    ALTER COLUMN CREATE_BY RENAME TO "create_by";
ALTER TABLE "points_key_config"
    ALTER COLUMN UPDATE_BY RENAME TO "update_by";
-- 积分密钥配置与密钥表
-- 中文注释：用于定义生成密钥的配置，以及实际生成的可兑换密钥

CREATE TABLE IF NOT EXISTS points_key_config
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(128)   NOT NULL,
    -- 永久积分，采用定点小数，遵循现有 user_points 精度
    permanent_points        DECIMAL(18, 6) NOT NULL DEFAULT 0,
    -- 订阅积分（用于扩展），当前不直接使用，可保留
    subscribe_points        DECIMAL(18, 6) NOT NULL DEFAULT 0,
    -- 订阅类型：MONTHLY, YEARLY, TRIAL
    subscribe_type          VARCHAR(32),
    -- 订阅时长（天）
    subscribe_duration_days INT            NOT NULL DEFAULT 0,
    -- 有效期开始/结束（密钥可使用的时间窗口）
    valid_from              TIMESTAMP,
    valid_to                TIMESTAMP,
    -- 是否禁用该配置（禁用后不允许继续基于该配置生成密钥）
    disabled                BOOLEAN        NOT NULL DEFAULT FALSE,

    create_time             TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by               VARCHAR(64),
    update_by               VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_points_key_conf_name ON points_key_config (name);

CREATE TABLE IF NOT EXISTS points_key
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 实际兑换用的Key字符串
    key_code    VARCHAR(128) NOT NULL,
    -- 关联配置ID
    config_id   BIGINT       NOT NULL,
    -- 生成时的配置快照，JSON，防止后续配置变更影响已生成密钥
    config_json JSON,
    -- 密钥是否已使用
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    used_time   TIMESTAMP,
    used_ip     VARCHAR(64),
    used_uid    VARCHAR(64),
    -- 密钥是否可用（禁用后不可兑换）
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    -- 过期时间（如存在），优先取配置 valid_to
    expire_time TIMESTAMP,

    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   VARCHAR(64),
    update_by   VARCHAR(64)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_points_key_code ON points_key (key_code);
CREATE INDEX IF NOT EXISTS idx_points_key_conf_id ON points_key (config_id);
CREATE INDEX IF NOT EXISTS idx_points_key_enabled ON points_key (enabled);
CREATE INDEX IF NOT EXISTS idx_points_key_used ON points_key (used);
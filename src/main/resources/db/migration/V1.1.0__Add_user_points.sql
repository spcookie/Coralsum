CREATE TABLE user_points
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

    open_user_id            BIGINT COMMENT 'open_user主键',

    permanent_points        INT         NOT NULL DEFAULT 0 COMMENT '永久积分',
    subscribe_points        INT         NOT NULL DEFAULT 0 COMMENT '订阅积分',

    tier                    VARCHAR(16) NOT NULL DEFAULT 'FREE' COMMENT '会员等级：FREE/PLUS/PRO',

    subscribe_expire_time   TIMESTAMP COMMENT '订阅到期时间',
    subscribe_duration_days INT                  DEFAULT 0 COMMENT '订阅累计时长（天）',

    subscribe_status        VARCHAR(32)          DEFAULT 'UNSUBSCRIBED' COMMENT '订阅状态：UNSUBSCRIBED/ACTIVE/EXPIRED',
    subscribe_type          VARCHAR(32) COMMENT '订阅类型：MONTHLY, YEARLY, TRIAL 等',

    create_time             TIMESTAMP            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             TIMESTAMP            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',

    create_by               VARCHAR(64) COMMENT '创建人',
    update_by               VARCHAR(64) COMMENT '修改人'
);

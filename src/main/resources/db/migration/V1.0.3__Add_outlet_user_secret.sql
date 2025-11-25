ALTER TABLE outlet_user
    ADD COLUMN secret VARCHAR(64) COMMENT '认证密钥';
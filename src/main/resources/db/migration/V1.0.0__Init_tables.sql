CREATE TABLE open_user
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid         VARCHAR(64) NOT NULL COMMENT 'UID',
    assign_role VARCHAR(64) COMMENT '分配角色',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by   VARCHAR(64) COMMENT '创建人',
    update_by   VARCHAR(64) COMMENT '修改人'
);

CREATE TABLE outlet_user
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_user_id BIGINT NOT NULL COMMENT '用户标识',
    user_source  VARCHAR(32) COMMENT '用户来源',
    source_code  VARCHAR(64) COMMENT '用户来源标识',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by    VARCHAR(64) COMMENT '创建人',
    update_by    VARCHAR(64) COMMENT '修改人'
);

CREATE INDEX idx_outlet_open_user_id ON outlet_user (open_user_id);
CREATE INDEX idx_outlet_source_code ON outlet_user (source_code);

CREATE TABLE generate_image_req_record
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_code      VARCHAR(64) NOT NULL COMMENT '用户标识',
    request_text   TEXT COMMENT '请求文本',
    request_image  BLOB COMMENT '请求图片',
    request_config JSON COMMENT '请求配置',
    retry_count    INT       DEFAULT 0 COMMENT '重试次数',
    image_ref      VARCHAR(512) COMMENT '生成图片地址',
    input_tokens   INT       DEFAULT 0 COMMENT '输入消耗Token',
    output_tokens  INT       DEFAULT 0 COMMENT '输出消耗Token',
    duration_ms    BIGINT    DEFAULT 0 COMMENT '生成耗时',
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by      VARCHAR(64) COMMENT '创建人',
    update_by      VARCHAR(64) COMMENT '修改人'
);

CREATE INDEX idx_generate_image_req_user_code ON generate_image_req_record (user_code);
CREATE INDEX idx_generate_image_req_create_time ON generate_image_req_record (create_time);

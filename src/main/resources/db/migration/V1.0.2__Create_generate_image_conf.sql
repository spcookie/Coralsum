CREATE TABLE generate_image_conf
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    open_user_id BIGINT COMMENT 'open_user主键',
    conf         JSON COMMENT '配置',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by    VARCHAR(64) COMMENT '创建人',
    update_by    VARCHAR(64) COMMENT '修改人'
);
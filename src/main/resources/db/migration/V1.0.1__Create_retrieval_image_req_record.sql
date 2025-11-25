CREATE TABLE retrieval_image_req_record
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    image_ref   VARCHAR(512) COMMENT '检索图片地址',
    ip          VARCHAR(64) COMMENT '请求IP',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by   VARCHAR(64) COMMENT '创建人',
    update_by   VARCHAR(64) COMMENT '修改人'
);

CREATE INDEX idx_retrieval_image_req_image_ref ON retrieval_image_req_record (image_ref);
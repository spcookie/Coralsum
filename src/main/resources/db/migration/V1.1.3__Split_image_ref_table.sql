CREATE TABLE generate_image_req_ref
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    record_id  BIGINT       NOT NULL COMMENT '生成记录ID',
    image_ref  VARCHAR(512) NOT NULL COMMENT '生成图片地址',
    image_size INT COMMENT '图片大小'
);

CREATE INDEX idx_generate_image_req_ref_record_id ON generate_image_req_ref (record_id);
CREATE INDEX idx_generate_image_req_ref_image_ref ON generate_image_req_ref (image_ref);

INSERT INTO generate_image_req_ref (record_id, image_ref)
SELECT id, image_ref
FROM generate_image_req_record
WHERE image_ref IS NOT NULL;

UPDATE generate_image_req_ref r
SET image_size = (
    SELECT CASE WHEN gr.image_size IS NOT NULL THEN gr.image_size[1] ELSE NULL END
    FROM generate_image_req_record gr
    WHERE gr.id = r.record_id
    );

ALTER TABLE generate_image_req_record
    DROP COLUMN image_size;
ALTER TABLE generate_image_req_record
    DROP COLUMN image_ref;

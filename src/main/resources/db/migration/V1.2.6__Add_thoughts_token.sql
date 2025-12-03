-- 为生成请求记录表增加思考令牌计数字段
ALTER TABLE generate_image_req_record
    ADD COLUMN thoughts_tokens INT DEFAULT 0 NOT NULL COMMENT '思考token数';

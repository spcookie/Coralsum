-- 为 open_user 表新增语言偏好字段
ALTER TABLE open_user
    ADD COLUMN language VARCHAR(16);


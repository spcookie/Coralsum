CREATE TABLE IF NOT EXISTS user_points_deduction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(128) NOT NULL,
    record_id BIGINT,
    image_ref VARCHAR(256),
    success BOOLEAN NOT NULL,
    points_deducted DECIMAL(19,4) NOT NULL,
    cost_detail JSON,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);


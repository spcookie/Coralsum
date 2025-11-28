CREATE TABLE IF NOT EXISTS nick_name_bitmap
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_source VARCHAR(32)     NOT NULL,
    base_name   VARCHAR(255)    NOT NULL,
    bitmap      VARBINARY(1250) NOT NULL,
    version     BIGINT,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_nick_bitmap ON nick_name_bitmap (user_source, base_name);
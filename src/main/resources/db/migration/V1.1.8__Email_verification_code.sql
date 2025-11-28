CREATE TABLE email_verification_code
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(128) NOT NULL,
    code        VARCHAR(16)  NOT NULL,
    purpose     VARCHAR(32)  NOT NULL,
    expire_time TIMESTAMP    NOT NULL,
    used        BOOLEAN   DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   VARCHAR(64),
    update_by   VARCHAR(64)
);

CREATE INDEX idx_email_code_email ON email_verification_code (email);
CREATE INDEX idx_email_code_purpose ON email_verification_code (purpose);
CREATE INDEX idx_email_code_expire ON email_verification_code (expire_time);
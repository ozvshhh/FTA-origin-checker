CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    company       VARCHAR(100) NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 원산지 점검 1건(화면의 "심사"). result_json 에 AI agent 결과(AgentCheckResult) 전체를 저장한다.
CREATE TABLE origin_checks
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    file_name     VARCHAR(255)  NOT NULL COMMENT '업로드한 원본 파일명',
    file_key      VARCHAR(255)  NOT NULL COMMENT '파일 저장소 키',
    file_size     BIGINT        NOT NULL,
    status        VARCHAR(20)   NOT NULL COMMENT 'QUEUED / IN_PROGRESS / COMPLETED / FAILED',
    agent_job_id  VARCHAR(100)  NULL COMMENT 'Upstage responses id',
    decision      VARCHAR(30)   NULL COMMENT 'NOT_APPLICABLE / SUPPLEMENT_REQUEST / REVIEW_REQUIRED / PREFERENTIAL',
    invoice_no    VARCHAR(100)  NULL,
    result_json   JSON          NULL,
    error_code    VARCHAR(50)   NULL,
    error_message VARCHAR(1000) NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    submitted_at  DATETIME(6)   NULL,
    completed_at  DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_origin_checks_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_origin_checks_user_id (user_id, id),
    INDEX idx_origin_checks_status (status, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

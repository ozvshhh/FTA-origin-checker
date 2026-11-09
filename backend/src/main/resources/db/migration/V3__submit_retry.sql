-- 제출 중 일시적 오류(네트워크·5xx·429)는 바로 실패시키지 않고 간격을 늘려 가며 다시 시도한다
ALTER TABLE origin_checks
    ADD COLUMN submit_attempts INT         NOT NULL DEFAULT 0 COMMENT '실패한 제출 시도 수' AFTER agent_file_id,
    ADD COLUMN next_submit_at  DATETIME(6) NULL COMMENT '이 시각 이후에 제출(재시도 대기)' AFTER submit_attempts;

UPDATE origin_checks SET next_submit_at = created_at WHERE next_submit_at IS NULL;

ALTER TABLE origin_checks
    ADD INDEX idx_origin_checks_submit (status, next_submit_at);

-- agent(Upstage) 쪽에 올린 파일 ID. 처리가 끝나면 워커가 파일을 지우고 NULL 로 둔다.
ALTER TABLE origin_checks
    ADD COLUMN agent_file_id VARCHAR(100) NULL COMMENT 'Upstage files id. 삭제 후 NULL' AFTER agent_job_id,
    ADD INDEX idx_origin_checks_agent_file (status, agent_file_id);

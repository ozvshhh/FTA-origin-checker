-- 담당자가 확인을 마치고 '완료 처리'한 시각. null 이면 진행 중(할 일 목록에 남는다)
ALTER TABLE origin_checks
    ADD COLUMN done_at DATETIME(6) NULL COMMENT '담당자 완료 처리 시각' AFTER completed_at;

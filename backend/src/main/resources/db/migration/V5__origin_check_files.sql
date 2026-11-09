-- 한 점검 건에 올린 원본 파일들. 점검에는 이 파일들을 올린 순서대로 이어 붙인 PDF(origin_checks.file_key)를 쓴다.
CREATE TABLE origin_check_files
(
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    check_id  BIGINT       NOT NULL,
    seq       INT          NOT NULL COMMENT '올린 순서 (0부터)',
    file_name VARCHAR(255) NOT NULL,
    file_key  VARCHAR(255) NOT NULL COMMENT '원본 파일 저장소 키',
    file_size BIGINT       NOT NULL,
    page_from INT          NOT NULL COMMENT '이어 붙인 PDF 에서의 첫 쪽 (1부터)',
    page_to   INT          NOT NULL COMMENT '이어 붙인 PDF 에서의 마지막 쪽',
    hint_type VARCHAR(20)  NULL COMMENT '올릴 때 화면이 파일명으로 짐작한 서류 종류. 판정에는 쓰지 않는다',
    PRIMARY KEY (id),
    CONSTRAINT fk_origin_check_files_check FOREIGN KEY (check_id) REFERENCES origin_checks (id) ON DELETE CASCADE,
    INDEX idx_origin_check_files_check (check_id, seq)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

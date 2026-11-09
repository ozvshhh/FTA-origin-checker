-- 목록에 보여 줄 요약(바이어·대표 품목·품목 수·목적국·선적일). 점검이 완료될 때 추출 결과에서 뽑아 저장한다.
-- 목록은 한 번에 수백 건을 읽으므로 조회할 때마다 result_json 을 파싱하지 않으려고 컬럼으로 둔다.
ALTER TABLE origin_checks
    ADD COLUMN consignee           VARCHAR(255) NULL COMMENT '인보이스 consignee(바이어)' AFTER invoice_no,
    ADD COLUMN primary_item        VARCHAR(255) NULL COMMENT '인보이스 첫 품목 품명' AFTER consignee,
    ADD COLUMN item_count          INT          NULL COMMENT '인보이스 품목 수' AFTER primary_item,
    ADD COLUMN destination_country VARCHAR(100) NULL COMMENT '목적국(수출신고필증, 없으면 원산지증명서 수입국)' AFTER item_count,
    ADD COLUMN shipped_on          DATE         NULL COMMENT '선적일(B/L 적재일, 없으면 원산지증명서 출항일)' AFTER destination_country;

-- 이미 완료된 건도 채운다. 날짜는 YYYY-MM-DD 로 온 값만 쓴다(다른 형식은 새로 점검할 때 애플리케이션이 맞춘다)
UPDATE origin_checks
SET consignee           = LEFT(NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."인보이스_추출"[0].consignee'))), ''), 255),
    primary_item        = LEFT(NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."인보이스_추출"[0].items[0].description'))), ''), 255),
    item_count          = NULLIF(JSON_LENGTH(JSON_EXTRACT(result_json, '$.extracted."인보이스_추출"[0].items')), 0),
    destination_country = LEFT(COALESCE(
            NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."수출신고필증_추출"[0].destination_country'))), ''),
            NULLIF(TRIM(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."원산지증명서_추출"[0].importing_country'))), '')), 100),
    shipped_on          = COALESCE(
            IF(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."BL_추출"[0].laden_on_board_date')) REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$',
               STR_TO_DATE(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."BL_추출"[0].laden_on_board_date')), '%Y-%m-%d'), NULL),
            IF(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."원산지증명서_추출"[0].departure_date')) REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$',
               STR_TO_DATE(JSON_UNQUOTE(JSON_EXTRACT(result_json, '$.extracted."원산지증명서_추출"[0].departure_date')), '%Y-%m-%d'), NULL))
WHERE status = 'COMPLETED' AND result_json IS NOT NULL;

package com.tradedoc.origincheck.check.dto;

import com.tradedoc.origincheck.check.OriginCheckFile;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * 점검 건에 올린 원본 파일 하나. page_from~page_to = 원본 PDF(GET …/file, 이어 붙인 PDF)에서 이 파일이 차지하는 쪽.
 * hint_type = 올릴 때 파일명으로 짐작한 서류 종류(없으면 null). AI 가 분류한 종류는 extracted_meta 의 page 로 화면이 계산한다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CheckFileResponse(String name, long size, int pageFrom, int pageTo, String hintType) {

    public static CheckFileResponse from(OriginCheckFile f) {
        return new CheckFileResponse(f.getFileName(), f.getFileSize(), f.getPageFrom(), f.getPageTo(), f.getHintType());
    }
}

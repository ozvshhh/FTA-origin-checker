package com.tradedoc.origincheck.check.storage;

import java.io.InputStream;
import java.nio.file.Path;

/** 원본 PDF 저장소. 지금은 로컬 디스크({@link LocalFileStorage}), 나중에 S3 등으로 바꿀 때 이 인터페이스만 구현한다. */
public interface FileStorage {

    /** @return 저장 키 */
    String save(InputStream in);

    /** agent 에 올리거나 내려줄 때 쓰는 로컬 경로 */
    Path path(String key);
}

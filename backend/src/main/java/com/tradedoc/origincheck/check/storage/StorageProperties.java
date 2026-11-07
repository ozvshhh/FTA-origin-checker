package com.tradedoc.origincheck.check.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.storage.dir — 업로드 PDF 를 둘 디렉터리 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String dir) {
}

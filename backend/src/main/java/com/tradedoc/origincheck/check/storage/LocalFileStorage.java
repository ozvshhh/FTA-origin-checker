package com.tradedoc.origincheck.check.storage;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/** 로컬 디스크 저장소. 키는 'yyyy/MM/dd/uuid.pdf'. */
@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(StorageProperties props) {
        this.root = Path.of(props.dir()).toAbsolutePath().normalize();
    }

    @Override
    public String save(InputStream in) {
        LocalDate d = LocalDate.now();
        String key = "%d/%02d/%02d/%s.pdf".formatted(d.getYear(), d.getMonthValue(), d.getDayOfMonth(), UUID.randomUUID());
        Path target = path(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패", e);
        }
        return key;
    }

    @Override
    public Path path(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 파일 키: " + key);
        }
        return p;
    }
}

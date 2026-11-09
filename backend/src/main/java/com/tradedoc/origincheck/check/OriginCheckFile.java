package com.tradedoc.origincheck.check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 점검 건에 올린 원본 파일 하나. 점검은 이 파일들을 올린 순서대로 이어 붙인 PDF 로 하고,
 * pageFrom~pageTo 로 이어 붙인 PDF 의 어느 쪽이 이 파일에서 왔는지 안다(화면이 AI 분류를 파일별로 보여 줄 때 쓴다).
 */
@Entity
@Table(name = "origin_check_files")
public class OriginCheckFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_id", nullable = false)
    private Long checkId;

    @Column(nullable = false)
    private int seq;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "page_from", nullable = false)
    private int pageFrom;

    @Column(name = "page_to", nullable = false)
    private int pageTo;

    @Column(name = "hint_type", length = 20)
    private String hintType;

    protected OriginCheckFile() {
    }

    public OriginCheckFile(Long checkId, int seq, String fileName, String fileKey, long fileSize,
                           int pageFrom, int pageTo, String hintType) {
        this.checkId = checkId;
        this.seq = seq;
        this.fileName = fileName;
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
        this.hintType = hintType;
    }

    public Long getId() { return id; }
    public Long getCheckId() { return checkId; }
    public int getSeq() { return seq; }
    public String getFileName() { return fileName; }
    public String getFileKey() { return fileKey; }
    public long getFileSize() { return fileSize; }
    public int getPageFrom() { return pageFrom; }
    public int getPageTo() { return pageTo; }
    public String getHintType() { return hintType; }
}

package com.tradedoc.origincheck.check;

import com.tradedoc.origincheck.agent.dto.Decision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 원산지 점검 1건(화면의 "심사"). 원본 PDF 는 파일 저장소에, agent 결과 전체는 result_json 에 둔다.
 * 목록에 보이는 값(decision, invoice_no)만 따로 컬럼으로 뺐다.
 */
@Entity
@Table(name = "origin_checks")
public class OriginCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CheckStatus status;

    @Column(name = "agent_job_id", length = 100)
    private String agentJobId;

    /** agent 쪽에 올린 파일. 처리가 끝나면 지우고 null 로 둔다 */
    @Column(name = "agent_file_id", length = 100)
    private String agentFileId;

    /** 일시적 오류로 실패한 제출 시도 수 */
    @Column(name = "submit_attempts", nullable = false)
    private int submitAttempts;

    /** QUEUED 인 건은 이 시각 이후에 제출한다(재시도 대기) */
    @Column(name = "next_submit_at")
    private Instant nextSubmitAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private Decision decision;

    @Column(name = "invoice_no", length = 100)
    private String invoiceNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected OriginCheck() {
    }

    public OriginCheck(Long userId, String title, String fileName, String fileKey, long fileSize) {
        this.userId = userId;
        this.title = title;
        this.fileName = fileName;
        this.fileKey = fileKey;
        this.fileSize = fileSize;
        this.status = CheckStatus.QUEUED;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        nextSubmitAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markSubmitted(String agentJobId, String agentFileId) {
        this.agentJobId = agentJobId;
        this.agentFileId = agentFileId;
        this.status = CheckStatus.IN_PROGRESS;
        this.submittedAt = Instant.now();
    }

    public void markCompleted(String resultJson, Decision decision, String invoiceNo) {
        this.resultJson = resultJson;
        this.decision = decision;
        this.invoiceNo = invoiceNo;
        this.status = CheckStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage == null || errorMessage.length() <= 1000
                ? errorMessage : errorMessage.substring(0, 1000);
        this.status = CheckStatus.FAILED;
        this.completedAt = Instant.now();
    }

    /** 제출이 일시적 오류로 실패했다. QUEUED 로 두고 {@code next} 이후에 다시 제출한다 */
    public void deferSubmit(Instant next) {
        this.submitAttempts++;
        this.nextSubmitAt = next;
    }

    public int getSubmitAttempts() {
        return submitAttempts;
    }

    public Instant getNextSubmitAt() {
        return nextSubmitAt;
    }

    /** agent 쪽 파일을 지웠다 */
    public void clearAgentFile() {
        this.agentFileId = null;
    }

    public String getAgentFileId() {
        return agentFileId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileKey() {
        return fileKey;
    }

    public long getFileSize() {
        return fileSize;
    }

    public CheckStatus getStatus() {
        return status;
    }

    public String getAgentJobId() {
        return agentJobId;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

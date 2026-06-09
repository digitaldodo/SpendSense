package com.spendsense.api.domain.finance;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_jobs")
public class ImportJob extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingestion_session_id")
    private IngestionSession ingestionSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 48)
    private IngestionSource source = IngestionSource.CSV;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    private ImportJobStatus status = ImportJobStatus.STARTED;

    @Column(name = "original_filename", nullable = false, length = 260)
    private String originalFilename;

    @Column(name = "file_checksum", nullable = false, length = 128)
    private String fileChecksum;

    @Column(name = "idempotency_key", length = 220)
    private String idempotencyKey;

    @Column(name = "mapping_json", nullable = false, columnDefinition = "text")
    private String mappingJson;

    @Column(name = "summary_json", columnDefinition = "text")
    private String summaryJson;

    @Column(name = "records_seen", nullable = false)
    private int recordsSeen;

    @Column(name = "records_imported", nullable = false)
    private int recordsImported;

    @Column(name = "records_duplicate", nullable = false)
    private int recordsDuplicate;

    @Column(name = "records_failed", nullable = false)
    private int recordsFailed;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ImportJob() {
    }

    public ImportJob(
            UserProfile userProfile,
            IngestionSession ingestionSession,
            Account account,
            String originalFilename,
            String fileChecksum,
            String idempotencyKey,
            String mappingJson
    ) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.ingestionSession = ingestionSession;
        this.account = account;
        this.originalFilename = originalFilename;
        this.fileChecksum = fileChecksum;
        this.idempotencyKey = idempotencyKey;
        this.mappingJson = mappingJson;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public IngestionSession getIngestionSession() {
        return ingestionSession;
    }

    public Account getAccount() {
        return account;
    }

    public IngestionSource getSource() {
        return source;
    }

    public ImportJobStatus getStatus() {
        return status;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getMappingJson() {
        return mappingJson;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public int getRecordsSeen() {
        return recordsSeen;
    }

    public int getRecordsImported() {
        return recordsImported;
    }

    public int getRecordsDuplicate() {
        return recordsDuplicate;
    }

    public int getRecordsFailed() {
        return recordsFailed;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void complete(
            int recordsSeen,
            int recordsImported,
            int recordsDuplicate,
            int recordsFailed,
            String summaryJson
    ) {
        this.status = recordsFailed > 0 ? ImportJobStatus.COMPLETED_WITH_ERRORS : ImportJobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.recordsSeen = recordsSeen;
        this.recordsImported = recordsImported;
        this.recordsDuplicate = recordsDuplicate;
        this.recordsFailed = recordsFailed;
        this.summaryJson = summaryJson;
    }

    public void fail(int recordsSeen, int recordsFailed, String summaryJson) {
        this.status = ImportJobStatus.FAILED;
        this.completedAt = Instant.now();
        this.recordsSeen = recordsSeen;
        this.recordsFailed = recordsFailed;
        this.summaryJson = summaryJson;
    }
}

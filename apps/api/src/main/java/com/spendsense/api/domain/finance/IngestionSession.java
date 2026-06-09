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
@Table(name = "ingestion_sessions")
public class IngestionSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 48)
    private IngestionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    private IngestionStatus status = IngestionStatus.STARTED;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "records_seen", nullable = false)
    private int recordsSeen;

    @Column(name = "records_imported", nullable = false)
    private int recordsImported;

    @Column(name = "records_duplicate", nullable = false)
    private int recordsDuplicate;

    @Column(name = "error_summary", columnDefinition = "text")
    private String errorSummary;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    protected IngestionSession() {
    }

    public IngestionSession(UserProfile userProfile, IngestionSource source, String metadataJson) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.source = source;
        this.metadataJson = metadataJson;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public IngestionSource getSource() {
        return source;
    }

    public IngestionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
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

    public String getErrorSummary() {
        return errorSummary;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void complete(int recordsSeen, int recordsImported, int recordsDuplicate) {
        this.status = IngestionStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.recordsSeen = recordsSeen;
        this.recordsImported = recordsImported;
        this.recordsDuplicate = recordsDuplicate;
    }
}

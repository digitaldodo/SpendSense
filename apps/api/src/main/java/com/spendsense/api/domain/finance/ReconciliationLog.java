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
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_logs")
public class ReconciliationLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id")
    private ImportJob importJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    private ImportJobStatus status;

    @Column(name = "records_seen", nullable = false)
    private int recordsSeen;

    @Column(name = "records_imported", nullable = false)
    private int recordsImported;

    @Column(name = "records_duplicate", nullable = false)
    private int recordsDuplicate;

    @Column(name = "records_failed", nullable = false)
    private int recordsFailed;

    @Column(name = "opening_balance", precision = 16, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", precision = 16, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "imported_balance_delta", nullable = false, precision = 16, scale = 2)
    private BigDecimal importedBalanceDelta = BigDecimal.ZERO;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    protected ReconciliationLog() {
    }

    public ReconciliationLog(
            UserProfile userProfile,
            ImportJob importJob,
            Account account,
            ImportJobStatus status,
            int recordsSeen,
            int recordsImported,
            int recordsDuplicate,
            int recordsFailed,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal importedBalanceDelta,
            String metadataJson
    ) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.importJob = importJob;
        this.account = account;
        this.status = status;
        this.recordsSeen = recordsSeen;
        this.recordsImported = recordsImported;
        this.recordsDuplicate = recordsDuplicate;
        this.recordsFailed = recordsFailed;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.importedBalanceDelta = importedBalanceDelta;
        this.metadataJson = metadataJson;
    }

    public ImportJob getImportJob() {
        return importJob;
    }

    public Account getAccount() {
        return account;
    }

    public ImportJobStatus getStatus() {
        return status;
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

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public BigDecimal getImportedBalanceDelta() {
        return importedBalanceDelta;
    }

    public String getMetadataJson() {
        return metadataJson;
    }
}

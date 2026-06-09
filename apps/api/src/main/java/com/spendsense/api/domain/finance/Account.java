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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "institution_name", nullable = false, length = 160)
    private String institutionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 48)
    private AccountType accountType;

    @Column(name = "account_mask", length = 16)
    private String accountMask;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "current_balance", nullable = false, precision = 16, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 16, scale = 2)
    private BigDecimal availableBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 48)
    private IngestionSource source = IngestionSource.MANUAL;

    @Column(name = "source_account_id", length = 180)
    private String sourceAccountId;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt = Instant.now();

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected Account() {
    }

    public Account(
            UserProfile userProfile,
            String displayName,
            String institutionName,
            AccountType accountType,
            String accountMask,
            String currency,
            BigDecimal currentBalance,
            IngestionSource source,
            String sourceAccountId
    ) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.displayName = displayName;
        this.institutionName = institutionName;
        this.accountType = accountType;
        this.accountMask = accountMask;
        this.currency = currency;
        this.currentBalance = currentBalance;
        this.availableBalance = currentBalance;
        this.source = source;
        this.sourceAccountId = sourceAccountId;
        this.lastSyncedAt = Instant.now();
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getAccountMask() {
        return accountMask;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public IngestionSource getSource() {
        return source;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }
}

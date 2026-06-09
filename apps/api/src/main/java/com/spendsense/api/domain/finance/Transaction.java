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
@Table(name = "transactions")
public class Transaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingestion_session_id")
    private IngestionSession ingestionSession;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 24)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransactionStatus status = TransactionStatus.POSTED;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "booked_at")
    private Instant bookedAt;

    @Column(name = "merchant_name", nullable = false, length = 180)
    private String merchantName;

    @Column(name = "merchant_normalized", nullable = false, length = 180)
    private String merchantNormalized;

    @Column(name = "description", length = 360)
    private String description;

    @Column(name = "reference", length = 220)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 48)
    private IngestionSource source;

    @Column(name = "source_transaction_id", length = 220)
    private String sourceTransactionId;

    @Column(name = "idempotency_key", length = 220)
    private String idempotencyKey;

    @Column(name = "dedupe_fingerprint", nullable = false, length = 128)
    private String dedupeFingerprint;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    protected Transaction() {
    }

    public Transaction(
            UserProfile userProfile,
            Account account,
            Category category,
            IngestionSession ingestionSession,
            BigDecimal amount,
            String currency,
            TransactionDirection direction,
            TransactionStatus status,
            Instant occurredAt,
            Instant bookedAt,
            String merchantName,
            String merchantNormalized,
            String description,
            String reference,
            IngestionSource source,
            String sourceTransactionId,
            String idempotencyKey,
            String dedupeFingerprint,
            String rawPayload
    ) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.account = account;
        this.category = category;
        this.ingestionSession = ingestionSession;
        this.amount = amount;
        this.currency = currency;
        this.direction = direction;
        this.status = status;
        this.occurredAt = occurredAt;
        this.bookedAt = bookedAt;
        this.merchantName = merchantName;
        this.merchantNormalized = merchantNormalized;
        this.description = description;
        this.reference = reference;
        this.source = source;
        this.sourceTransactionId = sourceTransactionId;
        this.idempotencyKey = idempotencyKey;
        this.dedupeFingerprint = dedupeFingerprint;
        this.rawPayload = rawPayload;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public Account getAccount() {
        return account;
    }

    public Category getCategory() {
        return category;
    }

    public IngestionSession getIngestionSession() {
        return ingestionSession;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantNormalized() {
        return merchantNormalized;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public IngestionSource getSource() {
        return source;
    }

    public String getSourceTransactionId() {
        return sourceTransactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDedupeFingerprint() {
        return dedupeFingerprint;
    }

    public String getRawPayload() {
        return rawPayload;
    }
}

package com.spendsense.api.domain.finance;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "merchant_normalized", nullable = false, length = 180)
    private String merchantNormalized;

    @Column(name = "merchant_name", nullable = false, length = 180)
    private String merchantName;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "cadence", nullable = false, length = 32)
    private String cadence;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "first_seen_on", nullable = false)
    private LocalDate firstSeenOn;

    @Column(name = "last_seen_on", nullable = false)
    private LocalDate lastSeenOn;

    @Column(name = "next_expected_on")
    private LocalDate nextExpectedOn;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "state", nullable = false, length = 32)
    private String state = "ACTIVE";

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    protected RecurringTransaction() {
    }
}

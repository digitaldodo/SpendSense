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
@Table(name = "budget_rollovers")
public class BudgetRollover extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "source_period_start", nullable = false)
    private LocalDate sourcePeriodStart;

    @Column(name = "source_period_end", nullable = false)
    private LocalDate sourcePeriodEnd;

    @Column(name = "target_period_start", nullable = false)
    private LocalDate targetPeriodStart;

    @Column(name = "target_period_end", nullable = false)
    private LocalDate targetPeriodEnd;

    @Column(name = "original_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "spent_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal spentAmount;

    @Column(name = "rollover_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal rolloverAmount;

    @Column(name = "state", nullable = false, length = 32)
    private String state = "MATERIALIZED";

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "materialized_at", nullable = false)
    private Instant materializedAt;

    protected BudgetRollover() {
    }
}

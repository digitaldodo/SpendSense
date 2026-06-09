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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "budget_history")
public class BudgetHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "action", nullable = false, length = 48)
    private String action;

    @Column(name = "previous_amount", precision = 16, scale = 2)
    private BigDecimal previousAmount;

    @Column(name = "new_amount", precision = 16, scale = 2)
    private BigDecimal newAmount;

    @Column(name = "previous_name", length = 160)
    private String previousName;

    @Column(name = "new_name", length = 160)
    private String newName;

    @Column(name = "previous_active")
    private Boolean previousActive;

    @Column(name = "new_active")
    private Boolean newActive;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "snapshot_json", columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "reason", length = 240)
    private String reason;

    protected BudgetHistory() {
    }

    public BudgetHistory(
            UserProfile userProfile,
            Budget budget,
            Category category,
            String action,
            BigDecimal previousAmount,
            BigDecimal newAmount,
            String previousName,
            String newName,
            Boolean previousActive,
            Boolean newActive,
            LocalDate periodStart,
            LocalDate periodEnd,
            String snapshotJson,
            String reason
    ) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.budget = budget;
        this.category = category;
        this.action = action;
        this.previousAmount = previousAmount;
        this.newAmount = newAmount;
        this.previousName = previousName;
        this.newName = newName;
        this.previousActive = previousActive;
        this.newActive = newActive;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.snapshotJson = snapshotJson;
        this.reason = reason;
    }

    public Budget getBudget() {
        return budget;
    }

    public Category getCategory() {
        return category;
    }

    public String getAction() {
        return action;
    }

    public BigDecimal getPreviousAmount() {
        return previousAmount;
    }

    public BigDecimal getNewAmount() {
        return newAmount;
    }

    public String getPreviousName() {
        return previousName;
    }

    public String getNewName() {
        return newName;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public String getReason() {
        return reason;
    }
}

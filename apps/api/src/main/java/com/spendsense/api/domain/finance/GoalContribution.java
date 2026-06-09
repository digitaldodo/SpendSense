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
@Table(name = "goal_contributions")
public class GoalContribution extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_goal_id", nullable = false)
    private SavingsGoal savingsGoal;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(name = "contributed_on", nullable = false)
    private LocalDate contributedOn;

    @Column(name = "source", nullable = false, length = 48)
    private String source = "MANUAL";

    @Column(name = "note", length = 240)
    private String note;

    protected GoalContribution() {
    }

    public GoalContribution(UserProfile userProfile, SavingsGoal savingsGoal, BigDecimal amount, LocalDate contributedOn, String note) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.savingsGoal = savingsGoal;
        this.amount = amount;
        this.contributedOn = contributedOn;
        this.note = note;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getContributedOn() {
        return contributedOn;
    }

    public String getNote() {
        return note;
    }
}

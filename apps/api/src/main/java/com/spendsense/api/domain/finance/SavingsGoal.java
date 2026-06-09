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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SavingsGoalStatus status = SavingsGoalStatus.ACTIVE;

    @Column(name = "color_token", nullable = false, length = 48)
    private String colorToken = "green";

    @Column(name = "icon_name", nullable = false, length = 64)
    private String iconName = "target";

    @Column(name = "completed_at")
    private Instant completedAt;

    protected SavingsGoal() {
    }

    public SavingsGoal(UserProfile userProfile, String name, BigDecimal targetAmount, BigDecimal currentAmount, String currency, LocalDate targetDate, String colorToken, String iconName) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount == null ? BigDecimal.ZERO : currentAmount;
        this.currency = currency == null || currency.isBlank() ? "INR" : currency;
        this.targetDate = targetDate;
        this.colorToken = colorToken == null || colorToken.isBlank() ? "green" : colorToken;
        this.iconName = iconName == null || iconName.isBlank() ? "target" : iconName;
        refreshCompletion();
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public SavingsGoalStatus getStatus() {
        return status;
    }

    public String getColorToken() {
        return colorToken;
    }

    public String getIconName() {
        return iconName;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void update(String name, BigDecimal targetAmount, String currency, LocalDate targetDate, SavingsGoalStatus status, String colorToken, String iconName) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.currency = currency == null || currency.isBlank() ? this.currency : currency;
        this.targetDate = targetDate;
        this.status = status == null ? this.status : status;
        this.colorToken = colorToken == null || colorToken.isBlank() ? this.colorToken : colorToken;
        this.iconName = iconName == null || iconName.isBlank() ? this.iconName : iconName;
        refreshCompletion();
    }

    public void addContribution(BigDecimal amount) {
        this.currentAmount = this.currentAmount.add(amount);
        refreshCompletion();
    }

    private void refreshCompletion() {
        if (currentAmount.compareTo(targetAmount) >= 0) {
            status = SavingsGoalStatus.COMPLETED;
            completedAt = completedAt == null ? Instant.now() : completedAt;
        } else if (status == SavingsGoalStatus.COMPLETED) {
            status = SavingsGoalStatus.ACTIVE;
            completedAt = null;
        }
    }
}

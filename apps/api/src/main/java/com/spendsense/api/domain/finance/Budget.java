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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class Budget extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 24)
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Column(name = "rollover_enabled", nullable = false)
    private boolean rolloverEnabled;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Budget() {
    }

    public Budget(UserProfile userProfile, Category category, String name, BigDecimal amount, String currency, LocalDate startsOn, boolean rolloverEnabled) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.currency = currency == null || currency.isBlank() ? "INR" : currency;
        this.startsOn = startsOn;
        this.rolloverEnabled = rolloverEnabled;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public boolean isRolloverEnabled() {
        return rolloverEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public void update(Category category, String name, BigDecimal amount, String currency, LocalDate startsOn, boolean rolloverEnabled) {
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.currency = currency == null || currency.isBlank() ? this.currency : currency;
        this.startsOn = startsOn;
        this.rolloverEnabled = rolloverEnabled;
    }

    public void deactivate(LocalDate endsOn) {
        this.active = false;
        this.endsOn = endsOn;
    }
}

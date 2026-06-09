package com.spendsense.api.domain.profile;

import com.spendsense.api.domain.BaseEntity;
import com.spendsense.api.domain.user.UserProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "financial_preferences")
public class FinancialPreferences extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    private UserProfile userProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_range")
    private SalaryRange salaryRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Column(name = "monthly_fixed_expenses", precision = 14, scale = 2)
    private BigDecimal monthlyFixedExpenses;

    @ElementCollection(targetClass = FinancialGoal.class)
    @CollectionTable(
            name = "financial_preference_goals",
            joinColumns = @JoinColumn(name = "financial_preferences_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "goal", nullable = false)
    private Set<FinancialGoal> goals = new LinkedHashSet<>();

    @ElementCollection(targetClass = SpendingHabit.class)
    @CollectionTable(
            name = "financial_preference_spending_habits",
            joinColumns = @JoinColumn(name = "financial_preferences_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "spending_habit", nullable = false)
    private Set<SpendingHabit> spendingHabits = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_comfort")
    private RiskComfort riskComfort;

    protected FinancialPreferences() {
    }

    public FinancialPreferences(UserProfile userProfile) {
        setId(UUID.randomUUID());
        this.userProfile = userProfile;
    }

    public SalaryRange getSalaryRange() {
        return salaryRange;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public BigDecimal getMonthlyFixedExpenses() {
        return monthlyFixedExpenses;
    }

    public Set<FinancialGoal> getGoals() {
        return goals;
    }

    public Set<SpendingHabit> getSpendingHabits() {
        return spendingHabits;
    }

    public RiskComfort getRiskComfort() {
        return riskComfort;
    }

    public void updateSalaryRange(SalaryRange salaryRange) {
        this.salaryRange = salaryRange;
    }

    public void updateEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public void updateMonthlyFixedExpenses(BigDecimal monthlyFixedExpenses) {
        this.monthlyFixedExpenses = monthlyFixedExpenses;
    }

    public void updateGoals(Set<FinancialGoal> goals) {
        this.goals.clear();
        this.goals.addAll(goals);
    }

    public void updateSpendingHabits(Set<SpendingHabit> spendingHabits) {
        this.spendingHabits.clear();
        this.spendingHabits.addAll(spendingHabits);
    }

    public void updateRiskComfort(RiskComfort riskComfort) {
        this.riskComfort = riskComfort;
    }

    public boolean isCompletionReady() {
        return salaryRange != null
                && employmentType != null
                && monthlyFixedExpenses != null
                && !goals.isEmpty()
                && !spendingHabits.isEmpty()
                && riskComfort != null;
    }
}

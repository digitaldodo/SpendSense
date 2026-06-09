package com.spendsense.api.dto.profile;

import com.spendsense.api.domain.profile.EmploymentType;
import com.spendsense.api.domain.profile.FinancialGoal;
import com.spendsense.api.domain.profile.RiskComfort;
import com.spendsense.api.domain.profile.SalaryRange;
import com.spendsense.api.domain.profile.SpendingHabit;
import java.math.BigDecimal;
import java.util.Set;

public record FinancialPreferencesResponse(
        SalaryRange salaryRange,
        EmploymentType employmentType,
        BigDecimal monthlyFixedExpenses,
        Set<FinancialGoal> goals,
        Set<SpendingHabit> spendingHabits,
        RiskComfort riskComfort
) {
}

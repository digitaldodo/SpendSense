package com.spendsense.api.dto.profile;

import com.spendsense.api.domain.profile.EmploymentType;
import com.spendsense.api.domain.profile.FinancialGoal;
import com.spendsense.api.domain.profile.OnboardingStep;
import com.spendsense.api.domain.profile.RiskComfort;
import com.spendsense.api.domain.profile.SalaryRange;
import com.spendsense.api.domain.profile.SpendingHabit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Set;

public record OnboardingProgressUpdateRequest(
        @Min(value = 0, message = "Current step is invalid.")
        @Max(value = 7, message = "Current step is invalid.")
        Integer currentStep,
        Set<OnboardingStep> completedSteps,
        SalaryRange salaryRange,
        EmploymentType employmentType,
        @DecimalMin(value = "0.00", message = "Monthly fixed expenses cannot be negative.")
        BigDecimal monthlyFixedExpenses,
        Set<FinancialGoal> goals,
        Set<SpendingHabit> spendingHabits,
        RiskComfort riskComfort
) {
}

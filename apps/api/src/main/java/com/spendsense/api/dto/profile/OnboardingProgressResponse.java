package com.spendsense.api.dto.profile;

import com.spendsense.api.domain.profile.OnboardingStep;
import java.time.Instant;
import java.util.Set;

public record OnboardingProgressResponse(
        int currentStep,
        Set<OnboardingStep> completedSteps,
        Instant completedAt
) {
}

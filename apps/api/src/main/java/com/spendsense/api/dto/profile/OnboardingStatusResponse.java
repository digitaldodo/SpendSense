package com.spendsense.api.dto.profile;

import com.spendsense.api.domain.profile.OnboardingStep;
import java.util.Set;

public record OnboardingStatusResponse(
        boolean completed,
        int currentStep,
        Set<OnboardingStep> completedSteps,
        String nextRoute
) {
}

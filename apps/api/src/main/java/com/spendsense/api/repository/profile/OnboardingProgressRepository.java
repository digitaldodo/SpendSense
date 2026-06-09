package com.spendsense.api.repository.profile;

import com.spendsense.api.domain.profile.OnboardingProgress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingProgressRepository extends JpaRepository<OnboardingProgress, UUID> {
    @EntityGraph(attributePaths = "completedSteps")
    Optional<OnboardingProgress> findByUserProfileId(UUID userProfileId);
}

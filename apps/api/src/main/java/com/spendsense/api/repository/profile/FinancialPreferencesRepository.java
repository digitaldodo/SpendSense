package com.spendsense.api.repository.profile;

import com.spendsense.api.domain.profile.FinancialPreferences;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialPreferencesRepository extends JpaRepository<FinancialPreferences, UUID> {
    @EntityGraph(attributePaths = {"goals", "spendingHabits"})
    Optional<FinancialPreferences> findByUserProfileId(UUID userProfileId);
}

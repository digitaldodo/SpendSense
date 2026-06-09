package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.SavingsGoal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {
    List<SavingsGoal> findByUserProfileIdOrderByStatusAscTargetDateAscCreatedAtAsc(UUID userProfileId);

    Optional<SavingsGoal> findByIdAndUserProfileId(UUID id, UUID userProfileId);
}

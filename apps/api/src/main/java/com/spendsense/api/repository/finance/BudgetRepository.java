package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.Budget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserProfileIdOrderByActiveDescStartsOnDescCreatedAtDesc(UUID userProfileId);

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserProfileIdAndActiveTrueOrderByStartsOnDescCreatedAtDesc(UUID userProfileId);

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserProfileIdAndActiveTrueAndRolloverEnabledTrueOrderByStartsOnDescCreatedAtDesc(UUID userProfileId);

    @EntityGraph(attributePaths = "category")
    Optional<Budget> findByIdAndUserProfileId(UUID id, UUID userProfileId);

    @Modifying
    @Query("update Budget b set b.category = :targetCategory where b.userProfile.id = :userProfileId and b.category.id = :sourceCategoryId")
    int moveBudgetsToCategory(UUID userProfileId, UUID sourceCategoryId, com.spendsense.api.domain.finance.Category targetCategory);
}

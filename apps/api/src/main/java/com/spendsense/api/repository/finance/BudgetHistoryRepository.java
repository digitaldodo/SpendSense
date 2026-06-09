package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.BudgetHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetHistoryRepository extends JpaRepository<BudgetHistory, UUID> {
    @EntityGraph(attributePaths = {"budget", "category"})
    List<BudgetHistory> findTop50ByUserProfileIdOrderByCreatedAtDesc(UUID userProfileId);
}

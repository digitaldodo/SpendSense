package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.GoalContribution;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {
    List<GoalContribution> findTop20BySavingsGoalIdOrderByContributedOnDescCreatedAtDesc(UUID savingsGoalId);

    @Query("""
            select coalesce(sum(c.amount), 0)
            from GoalContribution c
            where c.userProfile.id = :userProfileId
              and c.contributedOn >= :from
              and c.contributedOn < :to
            """)
    java.math.BigDecimal sumContributionsBetween(UUID userProfileId, LocalDate from, LocalDate to);
}

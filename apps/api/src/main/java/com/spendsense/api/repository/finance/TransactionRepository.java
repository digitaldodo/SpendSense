package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.TransactionDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    @EntityGraph(attributePaths = {"account", "category", "ingestionSession"})
    Optional<Transaction> findByIdAndUserProfileId(UUID id, UUID userProfileId);

    long countByUserProfileId(UUID userProfileId);

    long countByUserProfileIdAndSource(UUID userProfileId, com.spendsense.api.domain.finance.IngestionSource source);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userProfile.id = :userProfileId
              and t.direction = :direction
              and t.occurredAt >= :from
              and t.occurredAt < :to
              and t.status <> com.spendsense.api.domain.finance.TransactionStatus.EXCLUDED
            """)
    BigDecimal sumAmountForDirectionBetween(
            UUID userProfileId,
            TransactionDirection direction,
            Instant from,
            Instant to
    );
}

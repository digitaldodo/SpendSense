package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUserProfileIdOrderByCreatedAtAsc(UUID userProfileId);

    Optional<Account> findByIdAndUserProfileId(UUID id, UUID userProfileId);

    boolean existsByUserProfileIdAndSourceAccountId(UUID userProfileId, String sourceAccountId);

    long countByUserProfileId(UUID userProfileId);

    @Query("select coalesce(sum(a.currentBalance), 0) from Account a where a.userProfile.id = :userProfileId")
    BigDecimal sumCurrentBalance(UUID userProfileId);
}

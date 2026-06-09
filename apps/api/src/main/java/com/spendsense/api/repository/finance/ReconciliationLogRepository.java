package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.ReconciliationLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationLogRepository extends JpaRepository<ReconciliationLog, UUID> {
    List<ReconciliationLog> findByImportJobIdOrderByCreatedAtDesc(UUID importJobId);
}

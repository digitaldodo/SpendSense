package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.IngestionSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionSessionRepository extends JpaRepository<IngestionSession, UUID> {
}

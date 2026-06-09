package com.spendsense.api.repository.finance;

import com.spendsense.api.domain.finance.TransactionEdit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEditRepository extends JpaRepository<TransactionEdit, UUID> {
}

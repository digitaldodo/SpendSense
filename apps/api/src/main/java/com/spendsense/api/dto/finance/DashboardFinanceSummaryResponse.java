package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.util.List;

public record DashboardFinanceSummaryResponse(
        long accountCount,
        long transactionCount,
        boolean demoSeeded,
        BigDecimal totalBalance,
        BigDecimal monthSpend,
        BigDecimal monthIncome,
        List<AccountResponse> accounts,
        List<TransactionResponse> recentTransactions
) {
}

package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.BulkTransactionActionRequest;
import com.spendsense.api.dto.finance.BulkTransactionActionResponse;
import com.spendsense.api.dto.finance.DashboardFinanceSummaryResponse;
import com.spendsense.api.dto.finance.PageResponse;
import com.spendsense.api.dto.finance.TransactionDetailResponse;
import com.spendsense.api.dto.finance.TransactionFilterRequest;
import com.spendsense.api.dto.finance.TransactionResponse;
import com.spendsense.api.dto.finance.TransactionUpdateRequest;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> listTransactions(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @ModelAttribute TransactionFilterRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.listTransactions(principal, request),
                "Transactions loaded.",
                traceId
        ));
    }

    @GetMapping("/{transactionId}")
    ResponseEntity<ApiResponse<TransactionDetailResponse>> getTransaction(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID transactionId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getTransaction(principal, transactionId),
                "Transaction loaded.",
                traceId
        ));
    }

    @PatchMapping("/{transactionId}")
    ResponseEntity<ApiResponse<TransactionDetailResponse>> updateTransaction(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID transactionId,
            @RequestBody TransactionUpdateRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.updateTransaction(principal, transactionId, request),
                "Transaction updated.",
                traceId
        ));
    }

    @PostMapping("/bulk-actions")
    ResponseEntity<ApiResponse<BulkTransactionActionResponse>> bulkAction(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody BulkTransactionActionRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.bulkUpdateTransactions(principal, request),
                "Bulk transaction action applied.",
                traceId
        ));
    }

    @GetMapping("/dashboard-summary")
    ResponseEntity<ApiResponse<DashboardFinanceSummaryResponse>> dashboardSummary(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.dashboardSummary(principal),
                "Dashboard financial summary loaded.",
                traceId
        ));
    }
}

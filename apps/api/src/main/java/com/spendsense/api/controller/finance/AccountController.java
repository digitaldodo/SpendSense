package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.AccountMergeRequest;
import com.spendsense.api.dto.finance.AccountResponse;
import com.spendsense.api.dto.finance.BalanceCorrectionRequest;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    ResponseEntity<ApiResponse<List<AccountResponse>>> listAccounts(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.listAccounts(principal),
                "Accounts loaded.",
                traceId
        ));
    }

    @GetMapping("/{accountId}")
    ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID accountId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.getAccount(principal, accountId),
                "Account loaded.",
                traceId
        ));
    }

    @PostMapping("/{accountId}/merge")
    ResponseEntity<ApiResponse<AccountResponse>> mergeAccount(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountMergeRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.mergeAccount(principal, accountId, request),
                "Accounts merged safely.",
                traceId
        ));
    }

    @PatchMapping("/{accountId}/balance")
    ResponseEntity<ApiResponse<AccountResponse>> correctBalance(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID accountId,
            @Valid @RequestBody BalanceCorrectionRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.correctBalance(principal, accountId, request),
                "Account balance corrected.",
                traceId
        ));
    }
}

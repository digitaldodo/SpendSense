package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.CategoryMergeRequest;
import com.spendsense.api.dto.finance.CategoryRequest;
import com.spendsense.api.dto.finance.CategoryResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.listCategories(principal),
                "Categories loaded.",
                traceId
        ));
    }

    @PostMapping
    ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody CategoryRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.createCategory(principal, request),
                "Category created.",
                traceId
        ));
    }

    @PatchMapping("/{categoryId}")
    ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.updateCategory(principal, categoryId, request),
                "Category updated.",
                traceId
        ));
    }

    @PostMapping("/{categoryId}/merge")
    ResponseEntity<ApiResponse<CategoryResponse>> mergeCategory(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryMergeRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.mergeCategory(principal, categoryId, request),
                "Category merged.",
                traceId
        ));
    }
}

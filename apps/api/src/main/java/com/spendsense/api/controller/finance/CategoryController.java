package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.CategoryResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.CategoryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
}

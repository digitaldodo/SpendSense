package com.spendsense.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthResponsesIncludeSecurityAndCorrelationHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Correlation-Id", "phase-13-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Correlation-Id", "phase-13-test"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void protectedAdminRoutesRequireAuthenticationBeforeAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/admin/operations/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void generatedTraceIdLooksLikeUuidWhenHeaderIsMissing() throws Exception {
        String traceId = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("X-Trace-Id");

        assertThat(traceId).isNotBlank();
        assertThat(traceId).hasSize(36);
    }
}

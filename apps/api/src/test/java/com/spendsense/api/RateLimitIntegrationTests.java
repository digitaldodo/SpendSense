package com.spendsense.api;

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

@SpringBootTest(properties = {
        "spendsense.security.rate-limit.enabled=true",
        "spendsense.security.rate-limit.requests-per-minute=2",
        "spendsense.security.rate-limit.burst-capacity=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void rateLimitReturnsStructuredTooManyRequestsResponse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").with(request -> {
                    request.setRemoteAddr("10.13.0.1");
                    return request;
                }))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").with(request -> {
                    request.setRemoteAddr("10.13.0.1");
                    return request;
                }))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").with(request -> {
                    request.setRemoteAddr("10.13.0.1");
                    return request;
                }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}

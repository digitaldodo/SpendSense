package com.spendsense.api.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
class AiSafetyService {
    private static final int MAX_PROMPT_LENGTH = 2200;

    AiSafetyResult inspect(String prompt, String requestedIntent) {
        String sanitized = sanitize(prompt);
        List<String> flags = new ArrayList<>();
        String lower = sanitized.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "ignore previous", "ignore all", "system prompt", "developer message", "raw database", "show secrets", "api key", "jwt", "password")) {
            flags.add("PROMPT_INJECTION_ATTEMPT");
        }
        if (containsAny(lower, "which stock", "buy stock", "sell stock", "crypto", "mutual fund to buy", "trading", "guaranteed return", "multibagger")) {
            flags.add("INVESTMENT_ADVICE_REQUEST");
        }
        if (containsAny(lower, "depressed", "suicide", "self harm", "therapy", "therapist")) {
            flags.add("THERAPY_BOUNDARY");
        }
        String intent = normalizeIntent(requestedIntent, lower);
        boolean blocked = flags.contains("PROMPT_INJECTION_ATTEMPT") || flags.contains("INVESTMENT_ADVICE_REQUEST") || flags.contains("THERAPY_BOUNDARY");
        String safetyLevel = blocked ? "GUARDED" : flags.isEmpty() ? "CLEAR" : "CAUTION";
        return new AiSafetyResult(sanitized, intent, blocked, safetyLevel, List.copyOf(flags));
    }

    private String sanitize(String prompt) {
        String value = prompt == null ? "" : prompt;
        value = value.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
        value = value.replaceAll("(?i)bearer\\s+[a-z0-9._\\-]+", "bearer [redacted]");
        value = value.replaceAll("(?i)(api[_\\- ]?key|password|secret)\\s*[:=]\\s*\\S+", "$1=[redacted]");
        value = value.trim();
        if (value.length() > MAX_PROMPT_LENGTH) {
            return value.substring(0, MAX_PROMPT_LENGTH);
        }
        return value;
    }

    private String normalizeIntent(String requestedIntent, String lowerPrompt) {
        if (requestedIntent != null && !requestedIntent.isBlank()) {
            return requestedIntent.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        }
        if (containsAny(lowerPrompt, "overspend", "over spend", "spent too much", "budget pressure")) {
            return "OVERSPEND_EXPLANATION";
        }
        if (containsAny(lowerPrompt, "emi", "loan", "afford", "monthly installment")) {
            return "EMI_SAFETY";
        }
        if (containsAny(lowerPrompt, "category", "hurts my savings", "biggest spend")) {
            return "CATEGORY_SAVINGS_IMPACT";
        }
        if (containsAny(lowerPrompt, "health score", "financial health")) {
            return "HEALTH_SCORE_GUIDANCE";
        }
        if (containsAny(lowerPrompt, "changed", "last month", "compared")) {
            return "MONTHLY_CHANGE";
        }
        if (containsAny(lowerPrompt, "goal", "saving target")) {
            return "GOAL_GUIDANCE";
        }
        if (containsAny(lowerPrompt, "budget")) {
            return "BUDGET_GUIDANCE";
        }
        return "GENERAL_FINANCIAL_SUMMARY";
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

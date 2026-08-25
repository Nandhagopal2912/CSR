package com.supportrouter.strategy;

import com.supportrouter.model.Priority;
import com.supportrouter.model.SupportRequest;

public class RuleBasedPriorityStrategy implements PriorityStrategy {

    @Override
    public Priority determinePriority(SupportRequest request) {
        if (request == null || request.getMessage() == null) {
            throw new IllegalArgumentException("request and request message are required");
        }

        String message = request.getMessage().toLowerCase();
        if (message.contains("critical") || message.contains("security") || message.contains("outage")) {
            return Priority.CRITICAL;
        }
        if (message.contains("urgent") || message.contains("failed") || message.contains("error")
                || message.contains("blocked")) {
            return Priority.HIGH;
        }
        if (message.contains("question") || message.contains("information")) {
            return Priority.LOW;
        }
        return Priority.MEDIUM;
    }
}

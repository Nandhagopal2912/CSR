package com.supportrouter.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.supportrouter.model.Priority;
import com.supportrouter.model.SupportRequest;

public class RuleBasedPriorityStrategy implements PriorityStrategy {
    private static final Set<String> CRITICAL = Set.of("critical", "security", "outage", "breach", "hacked");
    private static final Set<String> HIGH = Set.of("urgent", "failed", "fail", "fails", "failing", "error",
            "errors", "blocked", "asap");
    private static final Set<String> LOW = Set.of("question", "questions", "information", "info");

    @Override
    public Priority determinePriority(SupportRequest request) {
        if (request == null || request.getMessage() == null) {
            throw new IllegalArgumentException("request and request message are required");
        }

        Set<String> words = Arrays.stream(request.getMessage().toLowerCase().split("[^a-z]+"))
                .collect(Collectors.toSet());
        if (!Collections.disjoint(words, CRITICAL)) {
            return Priority.CRITICAL;
        }
        if (!Collections.disjoint(words, HIGH)) {
            return Priority.HIGH;
        }
        if (!Collections.disjoint(words, LOW)) {
            return Priority.LOW;
        }
        return Priority.MEDIUM;
    }
}

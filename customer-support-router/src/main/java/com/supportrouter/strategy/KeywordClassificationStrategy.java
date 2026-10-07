package com.supportrouter.strategy;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

public class KeywordClassificationStrategy implements ClassificationStrategy {
    private static final Map<Category, Set<String>> KEYWORDS = new EnumMap<>(Map.of(
            Category.BILLING, Set.of("billing", "bill", "payment", "payments", "refund", "invoice", "charged",
                    "charge", "subscription", "upi"),
            Category.TECHNICAL, Set.of("technical", "error", "errors", "bug", "bugs", "crash", "crashed",
                    "crashes", "broken", "loading"),
            Category.ACCOUNT, Set.of("account", "login", "password", "signin", "locked", "username", "profile")));

    private final ClassificationStrategy fallback;

    public KeywordClassificationStrategy() {
        this(request -> Category.GENERAL);
    }

    public KeywordClassificationStrategy(ClassificationStrategy fallback) {
        if (fallback == null) {
            throw new IllegalArgumentException("fallback is required");
        }
        this.fallback = fallback;
    }

    @Override
    public Category classify(SupportRequest request) {
        if (request == null || request.getMessage() == null) {
            throw new IllegalArgumentException("request and request message are required");
        }
        Set<String> words = Arrays.stream(request.getMessage().toLowerCase().split("[^a-z]+"))
                .collect(Collectors.toSet());

        Map<Category, Long> scores = new EnumMap<>(Category.class);
        KEYWORDS.forEach((category, keywords) -> scores.put(category,
                words.stream().filter(keywords::contains).count()));

        long best = scores.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<Category> winners = scores.entrySet().stream()
                .filter(entry -> entry.getValue() == best)
                .map(Map.Entry::getKey)
                .toList();

        if (best == 0 || winners.size() > 1) {
            return fallback.classify(request);
        }
        return winners.get(0);
    }
}

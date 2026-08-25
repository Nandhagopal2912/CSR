package com.supportrouter.strategy;

import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

public class KeywordClassificationStrategy implements ClassificationStrategy {
    @Override
    public Category classify(SupportRequest request) {
        if (request == null || request.getMessage() == null) {
            throw new IllegalArgumentException("request and request message are required");
        }
        String message = request.getMessage().toLowerCase();
        if (message.contains("technical") || message.contains("error") || message.contains("bug")) {
            return Category.TECHNICAL;
        } else if (message.contains("billing") || message.contains("payment")) {
            return Category.BILLING;
        } else if (message.contains("account")) {
            return Category.ACCOUNT;
        } else {
            return Category.GENERAL;
        }
    }
}

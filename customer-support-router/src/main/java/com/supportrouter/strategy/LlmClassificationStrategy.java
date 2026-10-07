package com.supportrouter.strategy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

// Asks a small open-source model served locally by Ollama. Falls back to GENERAL if it is unavailable.
public class LlmClassificationStrategy implements ClassificationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(LlmClassificationStrategy.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PROMPT = """
            You route customer support messages into one category:
            BILLING - payments, refunds, invoices, charges
            TECHNICAL - errors, bugs, crashes, something not working
            ACCOUNT - login, password, profile, access to an account
            GENERAL - anything else
            Reply with exactly one word: BILLING, TECHNICAL, ACCOUNT, or GENERAL.

            Message: \"\"\"%s\"\"\"
            Category:""";

    private final URI endpoint;
    private final String model;
    private final HttpClient http;

    public LlmClassificationStrategy(String baseUrl, String model) {
        if (baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()) {
            throw new IllegalArgumentException("baseUrl and model are required");
        }
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/api/generate");
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @Override
    public Category classify(SupportRequest request) {
        try {
            String body = JSON.writeValueAsString(Map.of(
                    "model", model,
                    "prompt", PROMPT.formatted(request.getMessage()),
                    "stream", false,
                    "options", Map.of("temperature", 0)));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warn("LLM returned HTTP {}: {}", response.statusCode(), response.body());
                return Category.GENERAL;
            }
            String answer = JSON.readTree(response.body()).path("response").asText();
            Category category = parseCategory(answer);
            LOG.info("LLM ({}) classified request {} as {}", model, request.getRequestId(), category);
            return category;
        } catch (IOException exception) {
            LOG.warn("LLM unavailable at {} ({}); using GENERAL", endpoint, exception.toString());
            return Category.GENERAL;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Category.GENERAL;
        }
    }

    static Category parseCategory(String answer) {
        String text = answer == null ? "" : answer.toUpperCase();
        Category earliest = Category.GENERAL;
        int earliestIndex = Integer.MAX_VALUE;
        for (Category category : Category.values()) {
            int index = text.indexOf(category.name());
            if (index >= 0 && index < earliestIndex) {
                earliest = category;
                earliestIndex = index;
            }
        }
        return earliest;
    }
}

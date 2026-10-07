package com.supportrouter.strategy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

// Asks a small open-source model served locally by Ollama. Falls back to GENERAL if it is unavailable.
public class LlmClassificationStrategy implements ClassificationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(LlmClassificationStrategy.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SYSTEM_PROMPT = """
            You are a support ticket router. Pick the single best category for the user's message.
            GENERAL: general questions (hours, discounts, feedback, thanks) and anything that fits nothing else
            TECHNICAL: something is broken - errors, bugs, crashes, website or app not working
            BILLING: money - payments, charges, refunds, invoices, prices
            ACCOUNT: signing in, passwords, profile settings, account access""";
    // Constrains the model's output to one of the categories (Ollama structured outputs).
    private static final Map<String, Object> ANSWER_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("category", Map.of(
                    "type", "string",
                    "enum", List.of("GENERAL", "TECHNICAL", "BILLING", "ACCOUNT"))),
            "required", List.of("category"));

    private final URI endpoint;
    private final String model;
    private final HttpClient http;

    public LlmClassificationStrategy(String baseUrl, String model) {
        if (baseUrl == null || baseUrl.isBlank() || model == null || model.isBlank()) {
            throw new IllegalArgumentException("baseUrl and model are required");
        }
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/api/chat");
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @Override
    public Category classify(SupportRequest request) {
        try {
            String body = JSON.writeValueAsString(Map.of(
                    "model", model,
                    "stream", false,
                    "format", ANSWER_SCHEMA,
                    "options", Map.of("temperature", 0),
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", request.getMessage()))));
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
            String answer = JSON.readTree(response.body()).path("message").path("content").asText();
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

    public static Category parseCategory(String answer) {
        String text = answer == null ? "" : answer;
        try {
            JsonNode node = JSON.readTree(text);
            if (node != null && node.has("category")) {
                text = node.get("category").asText();
            }
        } catch (IOException notJson) {
            // Plain-text answers are handled below.
        }
        String upper = text.toUpperCase();
        return Arrays.stream(Category.values())
                .filter(category -> upper.contains(category.name()))
                .min((a, b) -> Integer.compare(upper.indexOf(a.name()), upper.indexOf(b.name())))
                .orElse(Category.GENERAL);
    }
}

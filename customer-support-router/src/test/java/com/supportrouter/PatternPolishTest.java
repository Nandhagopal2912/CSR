package com.supportrouter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.supportrouter.model.BillingTicket;
import com.supportrouter.model.Category;
import com.supportrouter.model.Customer;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.observer.TicketObserver;
import com.supportrouter.repository.InMemoryTicketRepository;
import com.supportrouter.repository.InMemoryUserRepository;
import com.supportrouter.service.SupportRouter;
import com.supportrouter.strategy.ClassificationStrategy;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.LlmClassificationStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

class PatternPolishTest {
    private static final Customer CUSTOMER = new Customer("C-1", "Ada", "ada@example.com");

    @Test
    void keywordStrategyMatchesWholeWordsOnly() {
        KeywordClassificationStrategy strategy = new KeywordClassificationStrategy();
        assertEquals(Category.TECHNICAL, strategy.classify(request("The app crashed with an error")));
        assertEquals(Category.GENERAL, strategy.classify(request("Can you debug my accountant's spreadsheet?")));
    }

    @Test
    void keywordStrategyAsksFallbackWhenNoMatchOrTie() {
        List<String> asked = new ArrayList<>();
        ClassificationStrategy fallback = request -> {
            asked.add(request.getMessage());
            return Category.ACCOUNT;
        };
        KeywordClassificationStrategy strategy = new KeywordClassificationStrategy(fallback);

        assertEquals(Category.BILLING, strategy.classify(request("I want a refund for my payment")));
        assertEquals(Category.ACCOUNT, strategy.classify(request("Nobody has replied to me in days")));
        assertEquals(Category.ACCOUNT, strategy.classify(request("Payment page shows an error")));
        assertEquals(List.of("Nobody has replied to me in days", "Payment page shows an error"), asked);
    }

    @Test
    void llmStrategyParsesModelAnswer() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer ollama = fakeOllama(200, "{\"response\":\" Billing.\"}", requestBody);
        try {
            LlmClassificationStrategy strategy = new LlmClassificationStrategy(url(ollama), "tiny-model");

            assertEquals(Category.BILLING, strategy.classify(request("I was charged twice")));

            JsonNode sent = new ObjectMapper().readTree(requestBody.get());
            assertEquals("tiny-model", sent.get("model").asText());
            assertFalse(sent.get("stream").asBoolean());
            assertTrue(sent.get("prompt").asText().contains("I was charged twice"));
        } finally {
            ollama.stop(0);
        }
    }

    @Test
    void llmStrategyFallsBackToGeneralOnErrors() throws IOException {
        HttpServer broken = fakeOllama(500, "{\"error\":\"model not found\"}", new AtomicReference<>());
        try {
            assertEquals(Category.GENERAL,
                    new LlmClassificationStrategy(url(broken), "missing").classify(request("help")));
        } finally {
            broken.stop(0);
        }

        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        assertEquals(Category.GENERAL, new LlmClassificationStrategy("http://localhost:" + closedPort, "m")
                .classify(request("help")));
    }

    @Test
    void criticalTicketsAreEscalatedButKeepTheirCategoryType() {
        SupportRouter router = new SupportRouter();

        Ticket critical = router.createTicket(request("Security breach: payment data exposed, critical"));
        assertInstanceOf(BillingTicket.class, critical);
        assertTrue(critical.isEscalated());
        assertEquals(Ticket.ESCALATION_TEAM, critical.getAssignedTeam());

        Ticket normal = router.createTicket(request("I need a refund"));
        assertFalse(normal.isEscalated());
        assertEquals("Billing Support Team", normal.getAssignedTeam());
    }

    @Test
    void injectedObserversAreNotified() {
        List<String> events = new ArrayList<>();
        TicketObserver recorder = (ticket, oldState, newState) -> events.add(newState.getName());
        SupportRouter router = new SupportRouter(new KeywordClassificationStrategy(),
                new RuleBasedPriorityStrategy(), new InMemoryTicketRepository(),
                InMemoryUserRepository.withDemoUsers(), List.of(recorder));

        String id = router.createTicket(request("I need a refund")).getTicketId();
        router.assignTicket(id, "ravi");

        assertEquals(List.of("ASSIGNED"), events);
        assertEquals(1, router.getStatusHistory(id).size());
    }

    private static SupportRequest request(String message) {
        return new SupportRequest("R-" + System.nanoTime(), CUSTOMER, message);
    }

    private static HttpServer fakeOllama(int status, String responseBody, AtomicReference<String> received)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/generate", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return server;
    }

    private static String url(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort();
    }
}

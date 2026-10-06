package com.supportrouter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportrouter.Application;

import io.javalin.Javalin;

class WebAppTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private Javalin server;
    private String baseUrl;

    @BeforeEach
    void start() {
        server = WebApp.create(Application.fromArgs(new String[] { Application.IN_MEMORY_FLAG })).start(0);
        baseUrl = "http://localhost:" + server.port();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void servesFrontend() throws Exception {
        HttpResponse<String> response = client().send(HttpRequest.newBuilder(URI.create(baseUrl + "/")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Customer Support Router"));
    }

    @Test
    void apiRequiresLogin() throws Exception {
        HttpResponse<String> response = get(client(), "/api/tickets");
        assertEquals(401, response.statusCode());
        assertEquals("Please log in.", JSON.readTree(response.body()).get("error").asText());
        assertEquals(401, post(client(), "/api/login", "{\"username\":\"nobody\"}").statusCode());
    }

    @Test
    void fullFlowAcrossRoles() throws Exception {
        HttpClient priya = loggedIn("priya");
        HttpResponse<String> created = post(priya, "/api/tickets",
                "{\"message\":\"payment failed <script>alert(1)</script>\"}");
        assertEquals(201, created.statusCode());
        JsonNode ticket = JSON.readTree(created.body());
        String id = ticket.get("id").asText();
        assertEquals("BILLING", ticket.get("category").asText());
        assertEquals("priya", ticket.get("customerId").asText());

        assertEquals(403, post(priya, "/api/tickets/" + id + "/assign", "{\"agent\":\"ravi\"}").statusCode());

        HttpClient sanjay = loggedIn("sanjay");
        HttpResponse<String> invalid = post(sanjay, "/api/tickets/" + id + "/status", "{\"action\":\"CLOSE\"}");
        assertEquals(409, invalid.statusCode());
        assertEquals("Cannot close an open ticket.", JSON.readTree(invalid.body()).get("error").asText());
        assertEquals(200, post(sanjay, "/api/tickets/" + id + "/assign", "{\"agent\":\"ravi\"}").statusCode());

        HttpClient meena = loggedIn("meena");
        assertEquals("[]", get(meena, "/api/tickets").body());
        assertEquals(403, get(meena, "/api/tickets/" + id).statusCode());

        HttpClient ravi = loggedIn("ravi");
        assertEquals(200, post(ravi, "/api/tickets/" + id + "/status", "{\"action\":\"start\"}").statusCode());
        JsonNode detail = JSON.readTree(get(ravi, "/api/tickets/" + id).body());
        assertEquals("INPROGRESS", detail.get("ticket").get("status").asText());
        assertEquals("ravi", detail.get("ticket").get("assignedAgent").asText());
        assertEquals(2, detail.get("history").size());

        assertEquals(404, get(sanjay, "/api/tickets/does-not-exist").statusCode());
        assertEquals(204, post(ravi, "/api/logout", "").statusCode());
        assertEquals(401, get(ravi, "/api/tickets").statusCode());
    }

    private HttpClient loggedIn(String username) throws Exception {
        HttpClient client = client();
        HttpResponse<String> response = post(client, "/api/login", "{\"username\":\"" + username + "\"}");
        assertEquals(200, response.statusCode(), response.body());
        return client;
    }

    private static HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

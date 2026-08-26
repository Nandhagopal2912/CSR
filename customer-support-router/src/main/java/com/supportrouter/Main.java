package com.supportrouter;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.UUID;

import com.supportrouter.model.Customer;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.repository.MySqlTicketRepository;
import com.supportrouter.repository.TicketRepository;
import com.supportrouter.service.SupportRouter;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        Customer customer = new Customer("C-1", "Ada Lovelace", "ada@example.com");
        SupportRequest request = new SupportRequest(
                "R-" + UUID.randomUUID(),
                customer,
                "I am having trouble logging into my account. Please assist.,account locked");

        TicketRepository repository = new MySqlTicketRepository(
                getConfiguration(dotenv, "DB_URL", "jdbc:mysql://localhost:3306/customer_support"),
                requireConfiguration(dotenv, "DB_USER"),
                getConfiguration(dotenv, "DB_PASSWORD", ""));
        SupportRouter router = new SupportRouter(
                new KeywordClassificationStrategy(),
                new RuleBasedPriorityStrategy(),
                repository);

        Ticket ticket = router.route(request);
        ticket.assign();
        ticket.start();
        ticket.resolve();
        ticket.close();
    }

    private static String requireConfiguration(Dotenv dotenv, String name) {
        String value = getConfiguration(dotenv, name, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set in .env or the environment");
        }
        return value;
    }

    private static String getConfiguration(Dotenv dotenv, String name, String defaultValue) {
        String environmentValue = System.getenv(name);
        return environmentValue != null ? environmentValue : dotenv.get(name, defaultValue);
    }
}
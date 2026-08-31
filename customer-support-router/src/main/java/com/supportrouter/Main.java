package com.supportrouter;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Scanner;
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

        TicketRepository repository = new MySqlTicketRepository(
                getConfiguration(dotenv, "DB_URL", "jdbc:mysql://localhost:3306/customer_support"),
                requireConfiguration(dotenv, "DB_USER"),
                getConfiguration(dotenv, "DB_PASSWORD", ""));
        SupportRouter router = new SupportRouter(
                new KeywordClassificationStrategy(),
                new RuleBasedPriorityStrategy(),
                repository);

        try (Scanner scanner = new Scanner(System.in)) {
            runInteractiveConsole(scanner, router);
        }
    }

    static void runInteractiveConsole(Scanner scanner, SupportRouter router) {
        System.out.println("=== Customer Support Router ===");
        System.out.println("1. Create a ticket");
        System.out.println("2. Exit");

        while (true) {
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            if ("2".equals(choice) || "exit".equalsIgnoreCase(choice) || "q".equalsIgnoreCase(choice)) {
                System.out.println("Bye!");
                return;
            }

            if (!"1".equals(choice) && !"create".equalsIgnoreCase(choice)) {
                System.out.println("Please enter 1 to create a ticket or 2 to exit.");
                continue;
            }

            System.out.print("Customer ID (optional): ");
            String customerId = scanner.nextLine().trim();
            System.out.print("Customer name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Customer email: ");
            String email = scanner.nextLine().trim();
            System.out.print("Describe the issue: ");
            String message = scanner.nextLine().trim();

            if (name.isBlank() || email.isBlank() || message.isBlank()) {
                System.out.println("Name, email, and issue description are required.");
                continue;
            }

            SupportRequest request = createSupportRequest(customerId, name, email, message);
            Ticket ticket = router.route(request);

            System.out.printf("Ticket created successfully. Ticket ID: %s%n", ticket.getTicketId());
            System.out.printf("Status: %s%n", ticket.getState().getClass().getSimpleName().replace("State", ""));
            System.out.println("Would you like to create another ticket? (y/n)");
            String again = scanner.nextLine().trim();
            if (!"y".equalsIgnoreCase(again) && !"yes".equalsIgnoreCase(again)) {
                return;
            }
        }
    }

    static SupportRequest createSupportRequest(String customerId, String name, String email, String message) {
        String normalizedCustomerId = customerId == null || customerId.isBlank() ? "C-" + UUID.randomUUID()
                : customerId;
        Customer customer = new Customer(normalizedCustomerId, name, email);
        return new SupportRequest("R-" + UUID.randomUUID(), customer, message);
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
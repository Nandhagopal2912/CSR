package com.supportrouter;

import io.github.cdimascio.dotenv.Dotenv;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

import com.supportrouter.model.Customer;
import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.repository.MySqlTicketRepository;
import com.supportrouter.repository.TicketRepository;
import com.supportrouter.service.SupportRouter;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

public class Main {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        while (true) {
            System.out.println();
            System.out.println("=== Customer Support Router ===");
            System.out.println("1. Create ticket");
            System.out.println("2. List tickets");
            System.out.println("3. View ticket (details + status history)");
            System.out.println("4. Update ticket status");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> createTicket(scanner, router);
                    case "2" -> listTickets(router);
                    case "3" -> viewTicket(scanner, router);
                    case "4" -> updateTicketStatus(scanner, router);
                    case "5", "q", "exit" -> {
                        System.out.println("Bye!");
                        return;
                    }
                    default -> System.out.println("Please choose an option from 1 to 5.");
                }
            } catch (IllegalStateException | IllegalArgumentException exception) {
                System.out.println("Error: " + exception.getMessage());
            }
        }
    }

    private static void createTicket(Scanner scanner, SupportRouter router) {
        String customerId = prompt(scanner, "Customer ID (optional): ");
        String name = prompt(scanner, "Customer name: ");
        String email = prompt(scanner, "Customer email: ");
        String message = prompt(scanner, "Describe the issue: ");

        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            System.out.println("Name, email, and issue description are required.");
            return;
        }

        Ticket ticket = router.route(createSupportRequest(customerId, name, email, message));
        System.out.printf("Ticket created successfully. Ticket ID: %s%n", ticket.getTicketId());
        System.out.printf("Status: %s%n", ticket.getStatusName());
    }

    private static void listTickets(SupportRouter router) {
        List<Ticket> tickets = router.listTickets();
        if (tickets.isEmpty()) {
            System.out.println("No tickets found.");
            return;
        }
        System.out.printf("%-8s %-10s %-9s %-11s %s%n", "ID", "CATEGORY", "PRIORITY", "STATUS", "CUSTOMER");
        for (Ticket ticket : tickets) {
            SupportRequest request = ticket.getSupportRequest();
            System.out.printf("%-8s %-10s %-9s %-11s %s%n", ticket.getTicketId(), request.getCategory(),
                    ticket.getPriority(), ticket.getStatusName(), request.getCustomer().getName());
        }
    }

    private static void viewTicket(Scanner scanner, SupportRouter router) {
        Optional<Ticket> found = findTicket(scanner, router);
        if (found.isEmpty()) {
            return;
        }
        Ticket ticket = found.get();
        SupportRequest request = ticket.getSupportRequest();
        System.out.println("----------------------------------------");
        System.out.printf("Ticket ID : %s%n", ticket.getTicketId());
        System.out.printf("Customer  : %s <%s>%n", request.getCustomer().getName(), request.getCustomer().getEmail());
        System.out.printf("Message   : %s%n", request.getMessage());
        System.out.printf("Category  : %s%n", request.getCategory());
        System.out.printf("Priority  : %s%n", ticket.getPriority());
        System.out.printf("Team      : %s%n", ticket.getAssignedTeam());
        System.out.printf("Status    : %s%n", ticket.getStatusName());
        System.out.println("Status history:");
        List<StatusChange> history = router.getStatusHistory(ticket.getTicketId());
        if (history.isEmpty()) {
            System.out.println("  (no status changes yet)");
        }
        for (StatusChange change : history) {
            System.out.printf("  %s  %s -> %s%n", change.changedAt().format(TIME_FORMAT), change.oldStatus(),
                    change.newStatus());
        }
        System.out.println("----------------------------------------");
    }

    private static void updateTicketStatus(Scanner scanner, SupportRouter router) {
        Optional<Ticket> found = findTicket(scanner, router);
        if (found.isEmpty()) {
            return;
        }
        Ticket ticket = found.get();
        System.out.printf("Current status: %s%n", ticket.getStatusName());
        System.out.println("a. Assign   s. Start   r. Resolve   c. Close");
        String action = prompt(scanner, "Choose an action: ").toLowerCase();

        switch (action) {
            case "a", "assign" -> ticket.assign();
            case "s", "start" -> ticket.start();
            case "r", "resolve" -> ticket.resolve();
            case "c", "close" -> ticket.close();
            default -> {
                System.out.println("Unknown action.");
                return;
            }
        }
        System.out.printf("Ticket %s is now %s.%n", ticket.getTicketId(), ticket.getStatusName());
    }

    private static Optional<Ticket> findTicket(Scanner scanner, SupportRouter router) {
        String ticketId = prompt(scanner, "Ticket ID: ");
        Optional<Ticket> ticket = router.findTicket(ticketId);
        if (ticket.isEmpty()) {
            System.out.println("No ticket found with ID " + ticketId + ".");
        }
        return ticket;
    }

    private static String prompt(Scanner scanner, String label) {
        System.out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
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
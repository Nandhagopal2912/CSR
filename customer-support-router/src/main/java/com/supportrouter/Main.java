package com.supportrouter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

import com.supportrouter.model.Customer;
import com.supportrouter.model.Permission;
import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.TicketAction;
import com.supportrouter.model.User;
import com.supportrouter.repository.UserRepository;
import com.supportrouter.service.SecuredSupportRouter;
import com.supportrouter.service.SupportService;

public class Main {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        Application app;
        try {
            app = Application.fromArgs(args);
        } catch (RuntimeException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            runInteractiveConsole(scanner, app.service(), app.users());
        }
    }

    static void runInteractiveConsole(Scanner scanner, SupportService service, UserRepository users) {
        System.out.println("=== Customer Support Router ===");
        while (true) {
            Optional<User> user = login(scanner, users);
            if (user.isEmpty()) {
                System.out.println("Bye!");
                return;
            }
            boolean exit = runSession(scanner, new SecuredSupportRouter(service, user.get()));
            if (exit) {
                System.out.println("Bye!");
                return;
            }
        }
    }

    private static Optional<User> login(Scanner scanner, UserRepository users) {
        while (true) {
            System.out.println();
            String username = prompt(scanner, "Username (or 'exit'): ");
            if (username == null || "exit".equalsIgnoreCase(username)) {
                return Optional.empty();
            }
            if (username.isBlank()) {
                continue;
            }
            try {
                Optional<User> user = users.findByUsername(username);
                if (user.isPresent()) {
                    System.out.printf("Welcome, %s (%s)%n", user.get().name(), user.get().role());
                    return user;
                }
                System.out.println("Unknown user: " + username);
            } catch (RuntimeException exception) {
                System.out.println("Error: " + exception.getMessage());
            }
        }
    }

    private static boolean runSession(Scanner scanner, SecuredSupportRouter service) {
        User user = service.getUser();
        while (true) {
            printMenu(user);
            String choice = prompt(scanner, "Choose an option: ");
            if (choice == null) {
                return true;
            }
            try {
                switch (choice) {
                    case "1" -> createTicket(scanner, service);
                    case "2" -> listTickets(service);
                    case "3" -> viewTicket(scanner, service);
                    case "4" -> assignTicket(scanner, service);
                    case "5" -> updateTicketStatus(scanner, service);
                    case "6" -> {
                        System.out.println("Logged out.");
                        return false;
                    }
                    case "0", "q", "exit" -> {
                        return true;
                    }
                    default -> System.out.println("Please choose an option from the menu.");
                }
            } catch (RuntimeException exception) {
                System.out.println("Error: " + exception.getMessage());
            }
        }
    }

    private static void printMenu(User user) {
        System.out.println();
        System.out.printf("=== Menu: %s (%s) ===%n", user.name(), user.role());
        if (user.can(Permission.CREATE_TICKET)) {
            System.out.println("1. Create ticket");
        }
        System.out.println("2. List tickets");
        System.out.println("3. View ticket (details + status history)");
        if (user.can(Permission.ASSIGN_TICKET)) {
            System.out.println("4. Assign ticket to an agent");
        }
        if (!allowedActions(user).isEmpty()) {
            System.out.println("5. Update ticket status");
        }
        System.out.println("6. Log out");
        System.out.println("0. Exit");
    }

    private static void createTicket(Scanner scanner, SecuredSupportRouter service) {
        User user = service.getUser();
        SupportRequest request;
        if (user.can(Permission.VIEW_ALL_TICKETS)) {
            String customerId = prompt(scanner, "Customer ID (optional): ");
            String name = prompt(scanner, "Customer name: ");
            String email = prompt(scanner, "Customer email: ");
            String message = prompt(scanner, "Describe the issue: ");
            if (isBlank(name) || isBlank(email) || isBlank(message)) {
                System.out.println("Name, email, and issue description are required.");
                return;
            }
            request = createSupportRequest(customerId, name, email, message);
        } else {
            String message = prompt(scanner, "Describe the issue: ");
            if (isBlank(message)) {
                System.out.println("Issue description is required.");
                return;
            }
            request = new SupportRequest("R-" + UUID.randomUUID(), user.asCustomer(), message);
        }

        Ticket ticket = service.createTicket(request);
        System.out.printf("Ticket created successfully. Ticket ID: %s%n", ticket.getTicketId());
        System.out.printf("Category: %s | Priority: %s | Team: %s | Status: %s%n",
                ticket.getSupportRequest().getCategory(), ticket.getPriority(), ticket.getAssignedTeam(),
                ticket.getStatusName());
    }

    private static void listTickets(SecuredSupportRouter service) {
        List<Ticket> tickets = service.listTickets();
        if (tickets.isEmpty()) {
            System.out.println("No tickets found.");
            return;
        }
        System.out.printf("%-8s %-10s %-9s %-11s %-10s %s%n", "ID", "CATEGORY", "PRIORITY", "STATUS", "AGENT",
                "CUSTOMER");
        for (Ticket ticket : tickets) {
            SupportRequest request = ticket.getSupportRequest();
            System.out.printf("%-8s %-10s %-9s %-11s %-10s %s%n", ticket.getTicketId(), request.getCategory(),
                    ticket.getPriority(), ticket.getStatusName(), orDash(ticket.getAssignedAgent()),
                    request.getCustomer().getName());
        }
    }

    private static void viewTicket(Scanner scanner, SecuredSupportRouter service) {
        String ticketId = prompt(scanner, "Ticket ID: ");
        Ticket ticket = service.getTicket(ticketId);
        SupportRequest request = ticket.getSupportRequest();
        System.out.println("----------------------------------------");
        System.out.printf("Ticket ID : %s%n", ticket.getTicketId());
        System.out.printf("Customer  : %s <%s>%n", request.getCustomer().getName(), request.getCustomer().getEmail());
        System.out.printf("Message   : %s%n", request.getMessage());
        System.out.printf("Category  : %s%n", request.getCategory());
        System.out.printf("Priority  : %s%n", ticket.getPriority());
        System.out.printf("Team      : %s%n", ticket.getAssignedTeam());
        System.out.printf("Agent     : %s%n", orDash(ticket.getAssignedAgent()));
        System.out.printf("Status    : %s%n", ticket.getStatusName());
        System.out.println("Status history:");
        List<StatusChange> history = service.getStatusHistory(ticket.getTicketId());
        if (history.isEmpty()) {
            System.out.println("  (no status changes yet)");
        }
        for (StatusChange change : history) {
            System.out.printf("  %s  %s -> %s%n", change.changedAt().format(TIME_FORMAT), change.oldStatus(),
                    change.newStatus());
        }
        System.out.println("----------------------------------------");
    }

    private static void assignTicket(Scanner scanner, SecuredSupportRouter service) {
        List<User> agents = service.listAgents();
        String ticketId = prompt(scanner, "Ticket ID: ");
        System.out.println("Agents:");
        for (User agent : agents) {
            System.out.printf("  %-10s %s%n", agent.username(), agent.name());
        }
        String agentUsername = prompt(scanner, "Agent username: ");
        Ticket ticket = service.assignTicket(ticketId, agentUsername);
        System.out.printf("Ticket %s is now %s and assigned to %s.%n", ticket.getTicketId(), ticket.getStatusName(),
                ticket.getAssignedAgent());
    }

    private static void updateTicketStatus(Scanner scanner, SecuredSupportRouter service) {
        List<TicketAction> actions = allowedActions(service.getUser());
        String ticketId = prompt(scanner, "Ticket ID: ");
        Ticket ticket = service.getTicket(ticketId);
        System.out.printf("Current status: %s%n", ticket.getStatusName());
        List<String> labels = new ArrayList<>();
        for (TicketAction action : actions) {
            labels.add(action.name().toLowerCase());
        }
        String input = prompt(scanner, "Action (" + String.join(" / ", labels) + "): ");
        ticket = service.changeStatus(ticketId, TicketAction.parse(input));
        System.out.printf("Ticket %s is now %s.%n", ticket.getTicketId(), ticket.getStatusName());
    }

    private static List<TicketAction> allowedActions(User user) {
        List<TicketAction> actions = new ArrayList<>();
        for (TicketAction action : TicketAction.values()) {
            if (user.can(action.getRequiredPermission())) {
                actions.add(action);
            }
        }
        return actions;
    }

    static SupportRequest createSupportRequest(String customerId, String name, String email, String message) {
        return new SupportRequest("R-" + UUID.randomUUID(), Customer.withOptionalId(customerId, name, email),
                message);
    }

    private static String prompt(Scanner scanner, String label) {
        System.out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}

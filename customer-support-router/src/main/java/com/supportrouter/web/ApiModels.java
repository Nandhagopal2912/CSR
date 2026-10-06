package com.supportrouter.web;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.User;

final class ApiModels {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ApiModels() {
    }

    record LoginRequest(String username) {
    }

    record CreateTicketRequest(String message, String customerId, String customerName, String customerEmail) {
    }

    record AssignRequest(String agent) {
    }

    record StatusRequest(String action) {
    }

    record ErrorResponse(String error) {
    }

    record UserView(String username, String name, String email, String role, List<String> permissions) {
        static UserView from(User user) {
            return new UserView(user.username(), user.name(), user.email(), user.role().name(),
                    user.role().getPermissions().stream().map(Enum::name).toList());
        }
    }

    record AgentView(String username, String name) {
        static AgentView from(User user) {
            return new AgentView(user.username(), user.name());
        }
    }

    record TicketView(String id, String category, String priority, String status, String message,
            String customerId, String customerName, String customerEmail, String assignedTeam, String assignedAgent) {
        static TicketView from(Ticket ticket) {
            SupportRequest request = ticket.getSupportRequest();
            return new TicketView(ticket.getTicketId(), request.getCategory().name(), ticket.getPriority().name(),
                    ticket.getStatusName(), request.getMessage(), request.getCustomer().getCustomerId(),
                    request.getCustomer().getName(), request.getCustomer().getEmail(), ticket.getAssignedTeam(),
                    ticket.getAssignedAgent());
        }
    }

    record HistoryView(String oldStatus, String newStatus, String changedAt) {
        static HistoryView from(StatusChange change) {
            return new HistoryView(change.oldStatus(), change.newStatus(), change.changedAt().format(TIME_FORMAT));
        }
    }

    record TicketDetailView(TicketView ticket, List<HistoryView> history) {
    }
}

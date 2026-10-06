package com.supportrouter.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.supportrouter.factory.TicketFactory;
import com.supportrouter.model.Category;
import com.supportrouter.model.Customer;
import com.supportrouter.model.Priority;
import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.state.TicketState;

public class MySqlTicketRepository implements TicketRepository {
    private static final String SELECT_TICKETS = "SELECT ticket_id, request_id, customer_id, customer_name, "
            + "customer_email, message, category, priority, assigned_team, assigned_agent, status FROM tickets";
    private final DatabaseConfig config;

    public MySqlTicketRepository(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        this.config = config;
    }

    @Override
    public Ticket create(Ticket ticket) {
        String sql = "INSERT INTO tickets (request_id, customer_id, customer_name, customer_email, message, "
                + "category, priority, assigned_team, assigned_agent, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = config.connect();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, ticket.getSupportRequest().getRequestId());
            bindTicketFields(statement, ticket, 2);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Database did not return a generated ticket ID");
                }
                ticket.setTicketId(generatedKeys.getString(1));
            }
            return ticket;
        } catch (SQLException exception) {
            throw databaseFailure("create ticket", exception);
        }
    }

    @Override
    public Optional<Ticket> findById(String ticketId) {
        List<Ticket> tickets = query(SELECT_TICKETS + " WHERE ticket_id = ?", ticketId, "find ticket");
        return tickets.stream().findFirst();
    }

    @Override
    public List<Ticket> findAll() {
        return query(SELECT_TICKETS + " ORDER BY ticket_id", null, "find all tickets");
    }

    @Override
    public List<Ticket> findByCustomerId(String customerId) {
        return query(SELECT_TICKETS + " WHERE customer_id = ? ORDER BY ticket_id", customerId,
                "find customer tickets");
    }

    @Override
    public List<Ticket> findByAssignedAgent(String agentUsername) {
        return query(SELECT_TICKETS + " WHERE assigned_agent = ? ORDER BY ticket_id", agentUsername,
                "find assigned tickets");
    }

    @Override
    public boolean update(Ticket ticket) {
        String sql = "UPDATE tickets SET customer_id = ?, customer_name = ?, customer_email = ?, message = ?, "
                + "category = ?, priority = ?, assigned_team = ?, assigned_agent = ?, status = ? WHERE ticket_id = ?";
        try (Connection connection = config.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTicketFields(statement, ticket, 1);
            statement.setString(10, ticket.getTicketId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw databaseFailure("update ticket", exception);
        }
    }

    @Override
    public boolean deleteById(String ticketId) {
        try (Connection connection = config.connect();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM tickets WHERE ticket_id = ?")) {
            statement.setString(1, ticketId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw databaseFailure("delete ticket", exception);
        }
    }

    @Override
    public void recordStatusChange(Ticket ticket, String oldStatus, String newStatus) {
        String sql = "INSERT INTO ticket_status_history (ticket_id, old_status, new_status) VALUES (?, ?, ?)";
        try (Connection connection = config.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, Long.parseLong(ticket.getTicketId()));
            statement.setString(2, oldStatus);
            statement.setString(3, newStatus);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure("record status change", exception);
        }
    }

    @Override
    public List<StatusChange> findStatusHistory(String ticketId) {
        String sql = "SELECT old_status, new_status, changed_at FROM ticket_status_history "
                + "WHERE ticket_id = ? ORDER BY changed_at, id";
        List<StatusChange> changes = new ArrayList<>();
        try (Connection connection = config.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    changes.add(new StatusChange(resultSet.getString("old_status"),
                            resultSet.getString("new_status"),
                            resultSet.getTimestamp("changed_at").toLocalDateTime()));
                }
            }
            return changes;
        } catch (SQLException exception) {
            throw databaseFailure("find status history", exception);
        }
    }

    private List<Ticket> query(String sql, String parameter, String operation) {
        List<Ticket> tickets = new ArrayList<>();
        try (Connection connection = config.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setString(1, parameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tickets.add(mapTicket(resultSet));
                }
            }
            return tickets;
        } catch (SQLException exception) {
            throw databaseFailure(operation, exception);
        }
    }

    private static void bindTicketFields(PreparedStatement statement, Ticket ticket, int offset) throws SQLException {
        var request = ticket.getSupportRequest();
        var customer = request.getCustomer();
        statement.setString(offset, customer.getCustomerId());
        statement.setString(offset + 1, customer.getName());
        statement.setString(offset + 2, customer.getEmail());
        statement.setString(offset + 3, request.getMessage());
        statement.setString(offset + 4, request.getCategory().name());
        statement.setString(offset + 5, request.getPriority().name());
        statement.setString(offset + 6, ticket.getAssignedTeam());
        if (ticket.getAssignedAgent() == null) {
            statement.setNull(offset + 7, Types.VARCHAR);
        } else {
            statement.setString(offset + 7, ticket.getAssignedAgent());
        }
        statement.setString(offset + 8, ticket.getStatusName());
    }

    private static Ticket mapTicket(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer(resultSet.getString("customer_id"), resultSet.getString("customer_name"),
                resultSet.getString("customer_email"));
        SupportRequest request = new SupportRequest(resultSet.getString("request_id"), customer,
                resultSet.getString("message"), Category.valueOf(resultSet.getString("category")),
                Priority.valueOf(resultSet.getString("priority")));
        Ticket ticket = TicketFactory.createTicket(request, TicketState.fromName(resultSet.getString("status")),
                resultSet.getString("assigned_agent"));
        ticket.setTicketId(resultSet.getString("ticket_id"));
        return ticket;
    }

    private static DataAccessException databaseFailure(String operation, SQLException exception) {
        return new DataAccessException("Unable to " + operation + ": " + exception.getMessage(), exception);
    }
}

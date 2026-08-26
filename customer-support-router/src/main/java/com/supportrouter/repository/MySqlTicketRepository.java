package com.supportrouter.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.supportrouter.factory.TicketFactory;
import com.supportrouter.model.Category;
import com.supportrouter.model.Customer;
import com.supportrouter.model.Priority;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.state.AssignedState;
import com.supportrouter.state.ClosedState;
import com.supportrouter.state.InProgressState;
import com.supportrouter.state.OpenState;
import com.supportrouter.state.ResolvedState;

public class MySqlTicketRepository implements TicketRepository {
    private static final String COLUMNS = "ticket_id, request_id, customer_id, customer_name, customer_email, message, "
            + "category, priority, assigned_team, status";
    private final String url;
    private final String username;
    private final String password;

    public MySqlTicketRepository(String url, String username, String password) {
        this.url = requireText(url, "url");
        this.username = requireText(username, "username");
        this.password = password == null ? "" : password;
    }

    @Override
    public Ticket create(Ticket ticket) {
        String sql = "INSERT INTO tickets (request_id, customer_id, customer_name, customer_email, message, "
                + "category, priority, assigned_team, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
            bindTicketForCreate(statement, ticket);
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
        String sql = "SELECT " + COLUMNS + " FROM tickets WHERE ticket_id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapTicket(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseFailure("find ticket", exception);
        }
    }

    @Override
    public List<Ticket> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM tickets ORDER BY ticket_id";
        List<Ticket> tickets = new ArrayList<>();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tickets.add(mapTicket(resultSet));
            }
            return tickets;
        } catch (SQLException exception) {
            throw databaseFailure("find all tickets", exception);
        }
    }

    @Override
    public boolean update(Ticket ticket) {
        String sql = "UPDATE tickets SET customer_id = ?, customer_name = ?, customer_email = ?, message = ?, "
                + "category = ?, priority = ?, assigned_team = ?, status = ? WHERE ticket_id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTicketWithoutId(statement, ticket);
            statement.setString(9, ticket.getTicketId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw databaseFailure("update ticket", exception);
        }
    }

    @Override
    public boolean deleteById(String ticketId) {
        try (Connection connection = connect();
                PreparedStatement statement = connection
                        .prepareStatement("DELETE FROM tickets WHERE ticket_id = ?")) {
            statement.setString(1, ticketId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw databaseFailure("delete ticket", exception);
        }
    }

    public void recordStatusChange(Ticket ticket, String oldStatus, String newStatus) {
        String sql = "INSERT INTO ticket_status_history (ticket_id, old_status, new_status) VALUES (?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, Long.parseLong(ticket.getTicketId()));
            statement.setString(2, oldStatus);
            statement.setString(3, newStatus);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure("record status change", exception);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static void bindTicketForCreate(PreparedStatement statement, Ticket ticket) throws SQLException {
        statement.setString(1, ticket.getSupportRequest().getRequestId());
        bindTicketWithoutId(statement, ticket, 2);
    }

    private static void bindTicketWithoutId(PreparedStatement statement, Ticket ticket) throws SQLException {
        bindTicketWithoutId(statement, ticket, 1);
    }

    private static void bindTicketWithoutId(PreparedStatement statement, Ticket ticket, int offset)
            throws SQLException {
        var request = ticket.getSupportRequest();
        var customer = request.getCustomer();
        statement.setString(offset, customer.getCustomerId());
        statement.setString(offset + 1, customer.getName());
        statement.setString(offset + 2, customer.getEmail());
        statement.setString(offset + 3, request.getMessage());
        statement.setString(offset + 4, request.getCategory().name());
        statement.setString(offset + 5, request.getPriority().name());
        statement.setString(offset + 6, ticket.getAssignedTeam());
        statement.setString(offset + 7, statusName(ticket));
    }

    private static Ticket mapTicket(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer(resultSet.getString("customer_id"), resultSet.getString("customer_name"),
                resultSet.getString("customer_email"));
        SupportRequest request = new SupportRequest(resultSet.getString("request_id"), customer,
                resultSet.getString("message"), Category.valueOf(resultSet.getString("category")),
                Priority.valueOf(resultSet.getString("priority")));
        Ticket ticket = TicketFactory.createTicket(request);
        ticket.setTicketId(resultSet.getString("ticket_id"));
        ticket.setState(stateFor(resultSet.getString("status")));
        return ticket;
    }

    private static String statusName(Ticket ticket) {
        return ticket.getState().getClass().getSimpleName().replace("State", "").toUpperCase();
    }

    private static com.supportrouter.state.TicketState stateFor(String status) {
        return switch (status) {
            case "OPEN" -> new OpenState();
            case "ASSIGNED" -> new AssignedState();
            case "INPROGRESS" -> new InProgressState();
            case "RESOLVED" -> new ResolvedState();
            case "CLOSED" -> new ClosedState();
            default -> throw new IllegalArgumentException("Unknown ticket status: " + status);
        };
    }

    private static IllegalStateException databaseFailure(String operation, SQLException exception) {
        return new IllegalStateException("Unable to " + operation, exception);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
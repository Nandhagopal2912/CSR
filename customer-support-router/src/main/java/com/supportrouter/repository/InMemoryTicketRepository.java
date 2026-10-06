package com.supportrouter.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.supportrouter.model.StatusChange;
import com.supportrouter.model.Ticket;

public class InMemoryTicketRepository implements TicketRepository {
    private final Map<String, Ticket> tickets = new LinkedHashMap<>();
    private final Map<String, List<StatusChange>> history = new LinkedHashMap<>();

    @Override
    public synchronized Ticket create(Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("ticket must not be null");
        }
        tickets.put(ticket.getTicketId(), ticket);
        return ticket;
    }

    @Override
    public synchronized Optional<Ticket> findById(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    @Override
    public synchronized List<Ticket> findAll() {
        return new ArrayList<>(tickets.values());
    }

    @Override
    public synchronized boolean update(Ticket ticket) {
        if (ticket == null || !tickets.containsKey(ticket.getTicketId())) {
            return false;
        }
        tickets.put(ticket.getTicketId(), ticket);
        return true;
    }

    @Override
    public synchronized boolean deleteById(String ticketId) {
        history.remove(ticketId);
        return tickets.remove(ticketId) != null;
    }

    @Override
    public synchronized void recordStatusChange(Ticket ticket, String oldStatus, String newStatus) {
        history.computeIfAbsent(ticket.getTicketId(), id -> new ArrayList<>())
                .add(new StatusChange(oldStatus, newStatus, LocalDateTime.now()));
    }

    @Override
    public synchronized List<StatusChange> findStatusHistory(String ticketId) {
        return new ArrayList<>(history.getOrDefault(ticketId, List.of()));
    }
}

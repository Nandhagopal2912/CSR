package com.supportrouter.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.supportrouter.model.Ticket;

public class InMemoryTicketRepository implements TicketRepository {
    private final Map<String, Ticket> tickets = new LinkedHashMap<>();

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
        return tickets.remove(ticketId) != null;
    }
}
package com.supportrouter.repository;

import java.util.List;
import java.util.Optional;

import com.supportrouter.model.StatusChange;
import com.supportrouter.model.Ticket;

public interface TicketRepository {
    Ticket create(Ticket ticket);

    Optional<Ticket> findById(String ticketId);

    List<Ticket> findAll();

    List<Ticket> findByCustomerId(String customerId);

    List<Ticket> findByAssignedAgent(String agentUsername);

    boolean update(Ticket ticket);

    boolean deleteById(String ticketId);

    void recordStatusChange(Ticket ticket, String oldStatus, String newStatus);

    List<StatusChange> findStatusHistory(String ticketId);
}

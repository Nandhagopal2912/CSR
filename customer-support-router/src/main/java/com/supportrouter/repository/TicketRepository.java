package com.supportrouter.repository;

import java.util.List;
import java.util.Optional;

import com.supportrouter.model.Ticket;

public interface TicketRepository {
    Ticket create(Ticket ticket);

    Optional<Ticket> findById(String ticketId);

    List<Ticket> findAll();

    boolean update(Ticket ticket);

    boolean deleteById(String ticketId);
}
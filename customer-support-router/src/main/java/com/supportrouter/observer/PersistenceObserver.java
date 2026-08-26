package com.supportrouter.observer;

import com.supportrouter.model.Ticket;
import com.supportrouter.repository.MySqlTicketRepository;
import com.supportrouter.repository.TicketRepository;
import com.supportrouter.state.TicketState;

public class PersistenceObserver implements TicketObserver {
    private final TicketRepository ticketRepository;

    public PersistenceObserver(TicketRepository ticketRepository) {
        if (ticketRepository == null) {
            throw new IllegalArgumentException("ticketRepository is required");
        }
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void update(Ticket ticket, TicketState oldState, TicketState newState) {
        ticketRepository.update(ticket);
        if (ticketRepository instanceof MySqlTicketRepository mysqlRepository) {
            mysqlRepository.recordStatusChange(ticket, statusName(oldState), statusName(newState));
        }
    }

    private static String statusName(TicketState state) {
        return state.getClass().getSimpleName().replace("State", "").toUpperCase();
    }
}
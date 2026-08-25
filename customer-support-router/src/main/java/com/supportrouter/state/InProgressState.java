package com.supportrouter.state;

import com.supportrouter.model.Ticket;

public class InProgressState implements TicketState {
    @Override
    public void assign(Ticket ticket) {
        System.out.println("Ticket is already assigned.");
    }

    @Override
    public void start(Ticket ticket) {
        System.out.println("Ticket is already in progress.");
    }

    @Override
    public void resolve(Ticket ticket) {
        System.out.println("Resolving the ticket...");
        ticket.setState(new ResolvedState());
    }

    @Override
    public void close(Ticket ticket) {
        throw new IllegalStateException("Resolve the ticket before closing it.");
    }

}

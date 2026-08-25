package com.supportrouter.state;

import com.supportrouter.model.Ticket;

public class OpenState implements TicketState {
    @Override
    public void assign(Ticket ticket) {
        ticket.setState(new AssignedState());
    }

    @Override
    public void start(Ticket ticket) {
        throw new IllegalStateException("Cannot start progress on an open ticket.");
    }

    @Override
    public void resolve(Ticket ticket) {
        throw new IllegalStateException("Cannot resolve an open ticket.");
    }

    @Override
    public void close(Ticket ticket) {
        throw new IllegalStateException("Cannot close an open ticket.");
    }

}

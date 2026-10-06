package com.supportrouter.state;

public class ClosedState implements TicketState {
    @Override
    public TicketState assign() {
        throw new IllegalStateException("Cannot assign a closed ticket.");
    }

    @Override
    public TicketState start() {
        throw new IllegalStateException("Cannot start a closed ticket.");
    }

    @Override
    public TicketState resolve() {
        throw new IllegalStateException("Cannot resolve a closed ticket.");
    }

    @Override
    public TicketState close() {
        throw new IllegalStateException("Ticket is already closed.");
    }

    @Override
    public String getName() {
        return "CLOSED";
    }
}

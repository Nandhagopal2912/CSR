package com.supportrouter.state;

public class ResolvedState implements TicketState {
    @Override
    public TicketState assign() {
        throw new IllegalStateException("Ticket is already resolved. Cannot assign.");
    }

    @Override
    public TicketState start() {
        throw new IllegalStateException("Ticket is already resolved. Cannot start.");
    }

    @Override
    public TicketState resolve() {
        throw new IllegalStateException("Ticket is already resolved.");
    }

    @Override
    public TicketState close() {
        return new ClosedState();
    }

    @Override
    public String getName() {
        return "RESOLVED";
    }
}

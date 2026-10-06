package com.supportrouter.state;

public class InProgressState implements TicketState {
    @Override
    public TicketState assign() {
        throw new IllegalStateException("Ticket is already assigned.");
    }

    @Override
    public TicketState start() {
        throw new IllegalStateException("Ticket is already in progress.");
    }

    @Override
    public TicketState resolve() {
        return new ResolvedState();
    }

    @Override
    public TicketState close() {
        throw new IllegalStateException("Resolve the ticket before closing it.");
    }

    @Override
    public String getName() {
        return "INPROGRESS";
    }
}

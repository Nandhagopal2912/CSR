package com.supportrouter.state;

public class AssignedState implements TicketState {
    @Override
    public TicketState assign() {
        throw new IllegalStateException("Ticket is already assigned.");
    }

    @Override
    public TicketState start() {
        return new InProgressState();
    }

    @Override
    public TicketState resolve() {
        throw new IllegalStateException("Cannot resolve a ticket that is not in progress.");
    }

    @Override
    public TicketState close() {
        throw new IllegalStateException("Cannot close a ticket that is not resolved.");
    }

    @Override
    public String getName() {
        return "ASSIGNED";
    }
}

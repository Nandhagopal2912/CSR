package com.supportrouter.state;

public class OpenState implements TicketState {
    @Override
    public TicketState assign() {
        return new AssignedState();
    }

    @Override
    public TicketState start() {
        throw new IllegalStateException("Cannot start an open ticket. Assign it first.");
    }

    @Override
    public TicketState resolve() {
        throw new IllegalStateException("Cannot resolve an open ticket.");
    }

    @Override
    public TicketState close() {
        throw new IllegalStateException("Cannot close an open ticket.");
    }

    @Override
    public String getName() {
        return "OPEN";
    }
}

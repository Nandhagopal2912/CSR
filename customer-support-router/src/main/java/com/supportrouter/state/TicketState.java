package com.supportrouter.state;

public interface TicketState {
    TicketState assign();

    TicketState start();

    TicketState resolve();

    TicketState close();

    String getName();

    static TicketState fromName(String name) {
        return switch (name) {
            case "OPEN" -> new OpenState();
            case "ASSIGNED" -> new AssignedState();
            case "INPROGRESS" -> new InProgressState();
            case "RESOLVED" -> new ResolvedState();
            case "CLOSED" -> new ClosedState();
            default -> throw new IllegalArgumentException("Unknown ticket status: " + name);
        };
    }
}

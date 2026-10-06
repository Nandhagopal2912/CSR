package com.supportrouter.model;

public enum TicketAction {
    START(Permission.START_TICKET),
    RESOLVE(Permission.RESOLVE_TICKET),
    CLOSE(Permission.CLOSE_TICKET);

    private final Permission requiredPermission;

    TicketAction(Permission requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public static TicketAction parse(String value) {
        for (TicketAction action : values()) {
            if (action.name().equalsIgnoreCase(value == null ? "" : value.trim())) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown action '" + value + "'. Use start, resolve, or close.");
    }

    public Permission getRequiredPermission() {
        return requiredPermission;
    }

    public void applyTo(Ticket ticket) {
        switch (this) {
            case START -> ticket.start();
            case RESOLVE -> ticket.resolve();
            case CLOSE -> ticket.close();
        }
    }
}

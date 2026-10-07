package com.supportrouter.model;

import com.supportrouter.state.TicketState;

public class BillingTicket extends Ticket {
    public BillingTicket(SupportRequest request) {
        super(request);
    }

    public BillingTicket(SupportRequest request, TicketState initialState, String assignedAgent) {
        super(request, initialState, assignedAgent);
    }

    @Override
    protected String defaultTeam() {
        return "Billing Support Team";
    }
}

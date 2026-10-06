package com.supportrouter.model;

import com.supportrouter.state.TicketState;

public class BillingTicket extends Ticket {
    public BillingTicket(SupportRequest request) {
        super(request);
    }

    public BillingTicket(SupportRequest request, TicketState initialState) {
        super(request, initialState);
    }
}

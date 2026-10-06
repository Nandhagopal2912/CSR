package com.supportrouter.model;

import com.supportrouter.state.TicketState;

public class AccountTicket extends Ticket {
    public AccountTicket(SupportRequest request) {
        super(request);
    }

    public AccountTicket(SupportRequest request, TicketState initialState) {
        super(request, initialState);
    }
}

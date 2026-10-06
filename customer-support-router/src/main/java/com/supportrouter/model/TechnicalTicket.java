package com.supportrouter.model;

import com.supportrouter.state.TicketState;

public class TechnicalTicket extends Ticket {
    public TechnicalTicket(SupportRequest request) {
        super(request);
    }

    public TechnicalTicket(SupportRequest request, TicketState initialState) {
        super(request, initialState);
    }
}

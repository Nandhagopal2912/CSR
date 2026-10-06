package com.supportrouter.model;

import com.supportrouter.state.TicketState;

public class GeneralTicket extends Ticket {
    public GeneralTicket(SupportRequest request) {
        super(request);
    }

    public GeneralTicket(SupportRequest request, TicketState initialState, String assignedAgent) {
        super(request, initialState, assignedAgent);
    }
}
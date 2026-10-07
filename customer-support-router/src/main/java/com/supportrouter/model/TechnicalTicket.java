package com.supportrouter.model;

import com.supportrouter.state.TicketState;

public class TechnicalTicket extends Ticket {
    public TechnicalTicket(SupportRequest request) {
        super(request);
    }

    public TechnicalTicket(SupportRequest request, TicketState initialState, String assignedAgent) {
        super(request, initialState, assignedAgent);
    }

    @Override
    protected String defaultTeam() {
        return "Technical Support Team";
    }
}

package com.supportrouter.factory;

import com.supportrouter.model.AccountTicket;
import com.supportrouter.model.BillingTicket;
import com.supportrouter.model.GeneralTicket;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.TechnicalTicket;
import com.supportrouter.model.Ticket;
import com.supportrouter.state.OpenState;
import com.supportrouter.state.TicketState;

public class TicketFactory {
    public static Ticket createTicket(SupportRequest request) {
        return createTicket(request, new OpenState(), null);
    }

    public static Ticket createTicket(SupportRequest request, TicketState initialState, String assignedAgent) {
        return switch (request.getCategory()) {
            case BILLING -> new BillingTicket(request, initialState, assignedAgent);
            case ACCOUNT -> new AccountTicket(request, initialState, assignedAgent);
            case TECHNICAL -> new TechnicalTicket(request, initialState, assignedAgent);
            case GENERAL -> new GeneralTicket(request, initialState, assignedAgent);
        };
    }
}

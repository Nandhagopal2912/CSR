package com.supportrouter.handler;

import com.supportrouter.model.Priority;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;

public class CriticalPriorityHandler extends SupportHandler {
    @Override
    protected boolean canHandle(SupportRequest request) {
        return request.getPriority() == Priority.CRITICAL;
    }

    @Override
    protected Ticket process(SupportRequest request) {
        Ticket ticket = super.process(request);
        ticket.escalate();
        return ticket;
    }
}

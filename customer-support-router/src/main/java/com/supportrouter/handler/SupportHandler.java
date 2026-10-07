package com.supportrouter.handler;

import com.supportrouter.factory.TicketFactory;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;

// Chain of Responsibility, with handle() as a Template Method: subclasses only decide canHandle()
// and can override process().
public abstract class SupportHandler {
    private SupportHandler next;

    public SupportHandler setNext(SupportHandler next) {
        this.next = next;
        return next;
    }

    public final Ticket handle(SupportRequest request) {
        if (canHandle(request)) {
            return process(request);
        }
        if (next == null) {
            throw new IllegalStateException("No handler could process category " + request.getCategory());
        }
        return next.handle(request);
    }

    protected abstract boolean canHandle(SupportRequest request);

    protected Ticket process(SupportRequest request) {
        return TicketFactory.createTicket(request);
    }
}

package com.supportrouter.handler;

import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;

public abstract class SupportHandler {
    protected SupportHandler next;

    public void setNext(SupportHandler next) {
        this.next = next;
    }

    public abstract Ticket handle(SupportRequest request);
}

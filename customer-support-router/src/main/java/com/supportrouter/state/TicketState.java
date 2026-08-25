package com.supportrouter.state;

import com.supportrouter.model.Ticket;

public interface TicketState {
    public void assign(Ticket ticket);

    public void start(Ticket ticket);

    void resolve(Ticket ticket);

    public void close(Ticket ticket);
}

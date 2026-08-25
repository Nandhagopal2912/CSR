package com.supportrouter.observer;

import com.supportrouter.model.Ticket;
import com.supportrouter.state.TicketState;

public interface TicketObserver {
    void update(Ticket ticket, TicketState oldState, TicketState newState);
}

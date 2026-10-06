package com.supportrouter.observer;

import com.supportrouter.model.Ticket;
import com.supportrouter.state.TicketState;

public class NotificationObserver implements TicketObserver {
    @Override
    public void update(Ticket ticket, TicketState oldState, TicketState newState) {
        System.out.println("Notification: Email to " + ticket.getSupportRequest().getCustomer().getEmail()
                + " - ticket " + ticket.getTicketId() + " is now " + newState.getName());
    }
}

package com.supportrouter.observer;
import com.supportrouter.model.Ticket;
import com.supportrouter.state.TicketState;
public class DashboardObserver implements TicketObserver {
    @Override
    public void update(Ticket ticket, TicketState oldState, TicketState newState) {
        System.out.println("Dashboard Update: Ticket " + ticket.getTicketId() + " changed from " + oldState.getClass().getSimpleName() + " to " + newState.getClass().getSimpleName());
    }

}

package com.supportrouter.state;
import com.supportrouter.model.Ticket;


public class ResolvedState implements TicketState {
    @Override
    public void assign(Ticket ticket) {
        System.out.println("Ticket is already resolved. Cannot assign.");
    }

    @Override
    public void start(Ticket ticket) {
        System.out.println("Ticket is already resolved. Cannot start.");
    }

    @Override
    public void resolve(Ticket ticket) {
        System.out.println("Ticket is already resolved.");
    }

    @Override
    public void close(Ticket ticket) {
        System.out.println("Closing the resolved ticket.");
        ticket.setState(new ClosedState());
    }

}

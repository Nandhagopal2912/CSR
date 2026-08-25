package com.supportrouter.state;
import com.supportrouter.model.Ticket;

public class AssignedState implements TicketState {
    @Override
    public void assign(Ticket ticket) {
        System.out.println("Ticket is already assigned.");
    }

    @Override
    public void start(Ticket ticket) {
        System.out.println("Starting work on the ticket.");
        ticket.setState(new InProgressState());
    }

    @Override
    public void resolve(Ticket ticket) {
        System.out.println("Cannot resolve a ticket that is not in progress.");
    }

    @Override
    public void close(Ticket ticket) {
        System.out.println("Cannot close a ticket that is not resolved.");
    }

}

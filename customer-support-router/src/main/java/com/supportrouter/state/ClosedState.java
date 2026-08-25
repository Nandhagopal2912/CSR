package com.supportrouter.state;
import com.supportrouter.model.Ticket;

public class ClosedState implements TicketState {
    @Override
    public void assign(Ticket ticket) {
        System.out.println("Cannot assign a closed ticket.");
    }

    @Override
    public void start(Ticket ticket) {
        System.out.println("Cannot start a closed ticket.");
    }

    @Override
    public void resolve(Ticket ticket) {
        System.out.println("Cannot resolve a closed ticket.");
    }

    @Override
    public void close(Ticket ticket) {
        System.out.println("Ticket is already closed.");
    }

}

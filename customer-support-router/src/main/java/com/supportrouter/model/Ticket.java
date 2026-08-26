package com.supportrouter.model;

import java.util.ArrayList;
import java.util.List;

import com.supportrouter.observer.TicketObserver;
import com.supportrouter.state.OpenState;
import com.supportrouter.state.TicketState;

public abstract class Ticket {
    private String ticketId;
    private SupportRequest supportRequest;
    private Priority priority;
    private String assignedTeam;
    private TicketState status;

    public Ticket(SupportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        this.ticketId = request.getRequestId();
        this.supportRequest = request;
        this.priority = request.getPriority();
        this.assignedTeam = request.getAssignedTeam();
        this.status = new OpenState();
    }

    public void assign() {
        status.assign(this);
    }

    public void start() {
        status.start(this);
    }

    public void resolve() {
        status.resolve(this);
    }

    public void close() {
        status.close(this);
    }

    public void setState(TicketState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("newState must not be null");
        }
        TicketState oldState = this.status;
        this.status = newState;
        notifyObservers(oldState, newState);
    }

    public TicketState getState() {
        return status;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId must not be blank");
        }
        this.ticketId = ticketId;
    }

    public SupportRequest getSupportRequest() {
        return supportRequest;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getAssignedTeam() {
        return assignedTeam;
    }

    private final List<TicketObserver> observers = new ArrayList<>();

    public void addObserver(TicketObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("observer must not be null");
        }
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(TicketObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(TicketState oldState, TicketState newState) {
        for (TicketObserver observer : observers) {
            observer.update(this, oldState, newState);
        }
    }
}

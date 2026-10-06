package com.supportrouter.model;

import java.util.ArrayList;
import java.util.List;

import com.supportrouter.observer.TicketObserver;
import com.supportrouter.state.OpenState;
import com.supportrouter.state.TicketState;

public abstract class Ticket {
    private String ticketId;
    private final SupportRequest supportRequest;
    private final Priority priority;
    private final String assignedTeam;
    private TicketState status;
    private String assignedAgent;
    private final List<TicketObserver> observers = new ArrayList<>();

    protected Ticket(SupportRequest request) {
        this(request, new OpenState(), null);
    }

    protected Ticket(SupportRequest request, TicketState initialState, String assignedAgent) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (initialState == null) {
            throw new IllegalArgumentException("initialState must not be null");
        }
        this.ticketId = request.getRequestId();
        this.supportRequest = request;
        this.priority = request.getPriority();
        this.assignedTeam = request.getAssignedTeam();
        this.status = initialState;
        this.assignedAgent = assignedAgent;
    }

    public void assign(String agentUsername) {
        if (agentUsername == null || agentUsername.isBlank()) {
            throw new IllegalArgumentException("An agent is required to assign a ticket");
        }
        TicketState next = status.assign();
        this.assignedAgent = agentUsername;
        transitionTo(next);
    }

    public void start() {
        transitionTo(status.start());
    }

    public void resolve() {
        transitionTo(status.resolve());
    }

    public void close() {
        transitionTo(status.close());
    }

    private void transitionTo(TicketState newState) {
        TicketState oldState = this.status;
        this.status = newState;
        notifyObservers(oldState, newState);
    }

    public TicketState getState() {
        return status;
    }

    public String getStatusName() {
        return status.getName();
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

    public String getAssignedAgent() {
        return assignedAgent;
    }

    public boolean isAssignedTo(String username) {
        return assignedAgent != null && assignedAgent.equals(username);
    }

    public boolean belongsTo(String customerId) {
        return supportRequest.getCustomer().getCustomerId().equals(customerId);
    }

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

package com.supportrouter.service;

import java.util.List;

import com.supportrouter.model.Permission;
import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.TicketAction;
import com.supportrouter.model.User;

// Proxy: checks the logged-in user's permissions before delegating to the real SupportService.
public class SecuredSupportRouter implements SupportService {
    private final SupportService delegate;
    private final User user;

    public SecuredSupportRouter(SupportService delegate, User user) {
        if (delegate == null || user == null) {
            throw new IllegalArgumentException("delegate and user are required");
        }
        this.delegate = delegate;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Ticket createTicket(SupportRequest request) {
        require(Permission.CREATE_TICKET);
        if (!user.can(Permission.VIEW_ALL_TICKETS)
                && !request.getCustomer().getCustomerId().equals(user.username())) {
            throw new AccessDeniedException("Customers can only create tickets for themselves.");
        }
        return delegate.createTicket(request);
    }

    @Override
    public List<Ticket> listTickets() {
        if (user.can(Permission.VIEW_ALL_TICKETS)) {
            return delegate.listTickets();
        }
        if (user.can(Permission.VIEW_ASSIGNED_TICKETS)) {
            return delegate.listTicketsForAgent(user.username());
        }
        if (user.can(Permission.VIEW_OWN_TICKETS)) {
            return delegate.listTicketsForCustomer(user.username());
        }
        throw denied("view tickets");
    }

    @Override
    public List<Ticket> listTicketsForCustomer(String customerId) {
        if (!user.can(Permission.VIEW_ALL_TICKETS)
                && !(user.can(Permission.VIEW_OWN_TICKETS) && user.username().equals(customerId))) {
            throw denied("view another customer's tickets");
        }
        return delegate.listTicketsForCustomer(customerId);
    }

    @Override
    public List<Ticket> listTicketsForAgent(String agentUsername) {
        if (!user.can(Permission.VIEW_ALL_TICKETS)
                && !(user.can(Permission.VIEW_ASSIGNED_TICKETS) && user.username().equals(agentUsername))) {
            throw denied("view another agent's tickets");
        }
        return delegate.listTicketsForAgent(agentUsername);
    }

    @Override
    public Ticket getTicket(String ticketId) {
        Ticket ticket = delegate.getTicket(ticketId);
        if (!canView(ticket)) {
            throw denied("view ticket " + ticketId);
        }
        return ticket;
    }

    @Override
    public List<StatusChange> getStatusHistory(String ticketId) {
        getTicket(ticketId);
        return delegate.getStatusHistory(ticketId);
    }

    @Override
    public Ticket assignTicket(String ticketId, String agentUsername) {
        require(Permission.ASSIGN_TICKET);
        return delegate.assignTicket(ticketId, agentUsername);
    }

    @Override
    public Ticket changeStatus(String ticketId, TicketAction action) {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        require(action.getRequiredPermission());
        Ticket ticket = getTicket(ticketId);
        if (!user.can(Permission.VIEW_ALL_TICKETS) && !ticket.isAssignedTo(user.username())) {
            throw denied(action.name().toLowerCase() + " a ticket that is not assigned to you");
        }
        return delegate.changeStatus(ticketId, action);
    }

    @Override
    public List<User> listAgents() {
        require(Permission.ASSIGN_TICKET);
        return delegate.listAgents();
    }

    private boolean canView(Ticket ticket) {
        return user.can(Permission.VIEW_ALL_TICKETS)
                || (user.can(Permission.VIEW_ASSIGNED_TICKETS) && ticket.isAssignedTo(user.username()))
                || (user.can(Permission.VIEW_OWN_TICKETS) && ticket.belongsTo(user.username()));
    }

    private void require(Permission permission) {
        if (!user.can(permission)) {
            throw denied(permission.name().toLowerCase().replace('_', ' '));
        }
    }

    private AccessDeniedException denied(String what) {
        return new AccessDeniedException(user.role() + " '" + user.username() + "' is not allowed to " + what + ".");
    }
}

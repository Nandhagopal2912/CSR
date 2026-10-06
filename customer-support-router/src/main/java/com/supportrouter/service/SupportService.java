package com.supportrouter.service;

import java.util.List;

import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.TicketAction;
import com.supportrouter.model.User;

public interface SupportService {
    Ticket createTicket(SupportRequest request);

    List<Ticket> listTickets();

    List<Ticket> listTicketsForCustomer(String customerId);

    List<Ticket> listTicketsForAgent(String agentUsername);

    Ticket getTicket(String ticketId);

    List<StatusChange> getStatusHistory(String ticketId);

    Ticket assignTicket(String ticketId, String agentUsername);

    Ticket changeStatus(String ticketId, TicketAction action);

    List<User> listAgents();
}

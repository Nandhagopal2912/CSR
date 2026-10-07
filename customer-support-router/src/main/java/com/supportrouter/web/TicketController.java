package com.supportrouter.web;

import java.util.UUID;

import com.supportrouter.model.Customer;
import com.supportrouter.model.Permission;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.TicketAction;
import com.supportrouter.model.User;
import com.supportrouter.repository.UserRepository;
import com.supportrouter.service.SecuredSupportRouter;
import com.supportrouter.service.SupportService;
import com.supportrouter.web.ApiModels.AgentView;
import com.supportrouter.web.ApiModels.AssignRequest;
import com.supportrouter.web.ApiModels.CreateTicketRequest;
import com.supportrouter.web.ApiModels.HistoryView;
import com.supportrouter.web.ApiModels.LoginRequest;
import com.supportrouter.web.ApiModels.StatusRequest;
import com.supportrouter.web.ApiModels.TicketDetailView;
import com.supportrouter.web.ApiModels.TicketView;
import com.supportrouter.web.ApiModels.UserView;

import io.javalin.http.Context;

// MVC controller: turns HTTP requests into calls on the secured service and returns JSON views.
class TicketController {
    private static final String SESSION_USER = "username";

    private final SupportService service;
    private final UserRepository users;

    TicketController(SupportService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    void login(Context ctx) {
        LoginRequest body = ctx.bodyAsClass(LoginRequest.class);
        String username = body.username() == null ? "" : body.username().trim();
        User user = users.findByUsername(username)
                .orElseThrow(() -> new NotAuthenticatedException("Unknown user: " + username));
        var existing = ctx.req().getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        ctx.sessionAttribute(SESSION_USER, user.username());
        ctx.json(UserView.from(user));
    }

    void logout(Context ctx) {
        var session = ctx.req().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ctx.status(204);
    }

    void me(Context ctx) {
        ctx.json(UserView.from(currentUser(ctx)));
    }

    void listTickets(Context ctx) {
        ctx.json(secured(ctx).listTickets().stream().map(TicketView::from).toList());
    }

    void getTicket(Context ctx) {
        SecuredSupportRouter service = secured(ctx);
        String id = ctx.pathParam("id");
        Ticket ticket = service.getTicket(id);
        ctx.json(new TicketDetailView(TicketView.from(ticket),
                service.getStatusHistory(id).stream().map(HistoryView::from).toList()));
    }

    void createTicket(Context ctx) {
        SecuredSupportRouter service = secured(ctx);
        User user = service.getUser();
        CreateTicketRequest body = ctx.bodyAsClass(CreateTicketRequest.class);
        if (isBlank(body.message())) {
            throw new IllegalArgumentException("Please describe the issue.");
        }

        SupportRequest request;
        if (user.can(Permission.VIEW_ALL_TICKETS)) {
            if (isBlank(body.customerName()) || isBlank(body.customerEmail())) {
                throw new IllegalArgumentException("Customer name and email are required.");
            }
            request = new SupportRequest("R-" + UUID.randomUUID(),
                    Customer.withOptionalId(body.customerId(), body.customerName(), body.customerEmail()),
                    body.message().trim());
        } else {
            request = new SupportRequest("R-" + UUID.randomUUID(), user.asCustomer(), body.message().trim());
        }

        ctx.status(201).json(TicketView.from(service.createTicket(request)));
    }

    void assignTicket(Context ctx) {
        AssignRequest body = ctx.bodyAsClass(AssignRequest.class);
        Ticket ticket = secured(ctx).assignTicket(ctx.pathParam("id"), body.agent());
        ctx.json(TicketView.from(ticket));
    }

    void changeStatus(Context ctx) {
        StatusRequest body = ctx.bodyAsClass(StatusRequest.class);
        Ticket ticket = secured(ctx).changeStatus(ctx.pathParam("id"), TicketAction.parse(body.action()));
        ctx.json(TicketView.from(ticket));
    }

    void listAgents(Context ctx) {
        ctx.json(secured(ctx).listAgents().stream().map(AgentView::from).toList());
    }

    private SecuredSupportRouter secured(Context ctx) {
        return new SecuredSupportRouter(service, currentUser(ctx));
    }

    private User currentUser(Context ctx) {
        String username = ctx.sessionAttribute(SESSION_USER);
        if (username == null) {
            throw new NotAuthenticatedException("Please log in.");
        }
        return users.findByUsername(username)
                .orElseThrow(() -> new NotAuthenticatedException("Your account no longer exists."));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package com.supportrouter.service;

import java.util.List;
import java.util.Optional;

import com.supportrouter.handler.AccountHandler;
import com.supportrouter.handler.BillingHandler;
import com.supportrouter.handler.GeneralHandler;
import com.supportrouter.handler.SupportHandler;
import com.supportrouter.handler.TechnicalHandler;
import com.supportrouter.model.StatusChange;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.observer.AuditObserver;
import com.supportrouter.observer.DashboardObserver;
import com.supportrouter.observer.NotificationObserver;
import com.supportrouter.observer.PersistenceObserver;
import com.supportrouter.observer.TicketObserver;
import com.supportrouter.repository.InMemoryTicketRepository;
import com.supportrouter.repository.TicketRepository;
import com.supportrouter.strategy.ClassificationStrategy;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.PriorityStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

public class SupportRouter {
    private final SupportHandler handlerChain;
    private final ClassificationStrategy classificationStrategy;
    private final PriorityStrategy priorityStrategy;
    private final TicketRepository ticketRepository;
    private final List<TicketObserver> observers;

    public SupportRouter() {
        this(new KeywordClassificationStrategy(), new RuleBasedPriorityStrategy(), new InMemoryTicketRepository());
    }

    public SupportRouter(ClassificationStrategy classificationStrategy, PriorityStrategy priorityStrategy) {
        this(classificationStrategy, priorityStrategy, new InMemoryTicketRepository());
    }

    public SupportRouter(ClassificationStrategy classificationStrategy, PriorityStrategy priorityStrategy,
            TicketRepository ticketRepository) {
        if (classificationStrategy == null || priorityStrategy == null) {
            throw new IllegalArgumentException("classification and priority strategies are required");
        }
        if (ticketRepository == null) {
            throw new IllegalArgumentException("ticketRepository is required");
        }
        this.classificationStrategy = classificationStrategy;
        this.priorityStrategy = priorityStrategy;
        this.ticketRepository = ticketRepository;
        this.observers = List.of(new AuditObserver(), new NotificationObserver(), new DashboardObserver(),
                new PersistenceObserver(ticketRepository));

        SupportHandler billing = new BillingHandler();
        SupportHandler technical = new TechnicalHandler();
        SupportHandler account = new AccountHandler();
        SupportHandler general = new GeneralHandler();

        billing.setNext(technical);
        technical.setNext(account);
        account.setNext(general);
        handlerChain = billing;
    }

    public Ticket route(SupportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        var category = classificationStrategy.classify(request);
        var priority = priorityStrategy.determinePriority(request);

        System.out.println();
        System.out.println("========================================");
        System.out.println("         SUPPORT REQUEST SUMMARY        ");
        System.out.println("========================================");
        System.out.printf("Request ID : %s%n", request.getRequestId());
        System.out.printf("Message    : %s%n", request.getMessage());
        System.out.printf("Category   : %s%n", category);
        System.out.printf("Priority   : %s%n", priority);
        System.out.println("========================================");

        SupportRequest classifiedRequest = request.withClassification(category, priority);
        Ticket ticket = handlerChain.handle(classifiedRequest);
        if (ticket == null) {
            throw new IllegalStateException("No handler could process category " + classifiedRequest.getCategory());
        }

        ticketRepository.create(ticket);
        attachObservers(ticket);
        return ticket;
    }

    public Optional<Ticket> findTicket(String ticketId) {
        Optional<Ticket> ticket = ticketRepository.findById(ticketId);
        ticket.ifPresent(this::attachObservers);
        return ticket;
    }

    public List<Ticket> listTickets() {
        List<Ticket> tickets = ticketRepository.findAll();
        tickets.forEach(this::attachObservers);
        return tickets;
    }

    public List<StatusChange> getStatusHistory(String ticketId) {
        return ticketRepository.findStatusHistory(ticketId);
    }

    private void attachObservers(Ticket ticket) {
        observers.forEach(ticket::addObserver);
    }
}

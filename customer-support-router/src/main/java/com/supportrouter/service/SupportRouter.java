package com.supportrouter.service;

import java.util.ArrayList;
import java.util.List;

import com.supportrouter.handler.AccountHandler;
import com.supportrouter.handler.BillingHandler;
import com.supportrouter.handler.CriticalPriorityHandler;
import com.supportrouter.handler.GeneralHandler;
import com.supportrouter.handler.SupportHandler;
import com.supportrouter.handler.TechnicalHandler;
import com.supportrouter.model.Customer;
import com.supportrouter.model.Role;
import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.TicketAction;
import com.supportrouter.model.User;
import com.supportrouter.observer.AuditObserver;
import com.supportrouter.observer.DashboardObserver;
import com.supportrouter.observer.NotificationObserver;
import com.supportrouter.observer.PersistenceObserver;
import com.supportrouter.observer.TicketObserver;
import com.supportrouter.repository.InMemoryTicketRepository;
import com.supportrouter.repository.InMemoryUserRepository;
import com.supportrouter.repository.TicketRepository;
import com.supportrouter.repository.UserRepository;
import com.supportrouter.strategy.ClassificationStrategy;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.PriorityStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

public class SupportRouter implements SupportService {
    private final SupportHandler handlerChain;
    private final ClassificationStrategy classificationStrategy;
    private final PriorityStrategy priorityStrategy;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final List<TicketObserver> observers;

    public SupportRouter() {
        this(new KeywordClassificationStrategy(), new RuleBasedPriorityStrategy(), new InMemoryTicketRepository(),
                InMemoryUserRepository.withDemoUsers(), defaultObservers());
    }

    public SupportRouter(ClassificationStrategy classificationStrategy, PriorityStrategy priorityStrategy,
            TicketRepository ticketRepository, UserRepository userRepository, List<TicketObserver> observers) {
        if (classificationStrategy == null || priorityStrategy == null) {
            throw new IllegalArgumentException("classification and priority strategies are required");
        }
        if (ticketRepository == null || userRepository == null || observers == null) {
            throw new IllegalArgumentException("ticketRepository, userRepository, and observers are required");
        }
        this.classificationStrategy = classificationStrategy;
        this.priorityStrategy = priorityStrategy;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;

        List<TicketObserver> allObservers = new ArrayList<>(observers);
        allObservers.add(new PersistenceObserver(ticketRepository));
        this.observers = List.copyOf(allObservers);

        SupportHandler critical = new CriticalPriorityHandler();
        critical.setNext(new BillingHandler())
                .setNext(new TechnicalHandler())
                .setNext(new AccountHandler())
                .setNext(new GeneralHandler());
        handlerChain = critical;
    }

    public static List<TicketObserver> defaultObservers() {
        return List.of(new AuditObserver(), new NotificationObserver(), new DashboardObserver());
    }

    @Override
    public Ticket createTicket(SupportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String email = request.getCustomer().getEmail();
        if (!Customer.isValidEmail(email)) {
            throw new IllegalArgumentException("'" + email + "' is not a valid email address.");
        }

        var category = classificationStrategy.classify(request);
        var priority = priorityStrategy.determinePriority(request);
        Ticket ticket = handlerChain.handle(request.withClassification(category, priority));

        ticketRepository.create(ticket);
        attachObservers(ticket);
        return ticket;
    }

    @Override
    public List<Ticket> listTickets() {
        return withObservers(ticketRepository.findAll());
    }

    @Override
    public List<Ticket> listTicketsForCustomer(String customerId) {
        return withObservers(ticketRepository.findByCustomerId(customerId));
    }

    @Override
    public List<Ticket> listTicketsForAgent(String agentUsername) {
        return withObservers(ticketRepository.findByAssignedAgent(agentUsername));
    }

    @Override
    public Ticket getTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("No ticket found with ID " + ticketId + "."));
        attachObservers(ticket);
        return ticket;
    }

    @Override
    public List<StatusChange> getStatusHistory(String ticketId) {
        return ticketRepository.findStatusHistory(ticketId);
    }

    @Override
    public Ticket assignTicket(String ticketId, String agentUsername) {
        User agent = userRepository.findByUsername(agentUsername)
                .filter(user -> user.role() == Role.AGENT)
                .orElseThrow(() -> new IllegalArgumentException("No agent found with username " + agentUsername + "."));
        Ticket ticket = getTicket(ticketId);
        ticket.assign(agent.username());
        return ticket;
    }

    @Override
    public Ticket changeStatus(String ticketId, TicketAction action) {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        Ticket ticket = getTicket(ticketId);
        action.applyTo(ticket);
        return ticket;
    }

    @Override
    public List<User> listAgents() {
        return userRepository.findByRole(Role.AGENT);
    }

    private List<Ticket> withObservers(List<Ticket> tickets) {
        tickets.forEach(this::attachObservers);
        return tickets;
    }

    private void attachObservers(Ticket ticket) {
        observers.forEach(ticket::addObserver);
    }
}

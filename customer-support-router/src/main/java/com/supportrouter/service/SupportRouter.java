package com.supportrouter.service;

import com.supportrouter.handler.AccountHandler;
import com.supportrouter.handler.BillingHandler;
import com.supportrouter.handler.GeneralHandler;
import com.supportrouter.handler.SupportHandler;
import com.supportrouter.handler.TechnicalHandler;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.observer.AuditObserver;
import com.supportrouter.observer.DashboardObserver;
import com.supportrouter.observer.NotificationObserver;
import com.supportrouter.strategy.ClassificationStrategy;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.PriorityStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

public class SupportRouter {
    private final SupportHandler handlerChain;
    private final ClassificationStrategy classificationStrategy;
    private final PriorityStrategy priorityStrategy;

    public SupportRouter() {
        this(new KeywordClassificationStrategy(), new RuleBasedPriorityStrategy());
    }

    public SupportRouter(ClassificationStrategy classificationStrategy, PriorityStrategy priorityStrategy) {
        if (classificationStrategy == null || priorityStrategy == null) {
            throw new IllegalArgumentException("classification and priority strategies are required");
        }
        this.classificationStrategy = classificationStrategy;
        this.priorityStrategy = priorityStrategy;

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

        ticket.addObserver(new AuditObserver());
        ticket.addObserver(new NotificationObserver());
        ticket.addObserver(new DashboardObserver());

        return ticket;
    }

}

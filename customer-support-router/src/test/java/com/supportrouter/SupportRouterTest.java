package com.supportrouter;

import com.supportrouter.model.AccountTicket;
import com.supportrouter.model.BillingTicket;
import com.supportrouter.model.Category;
import com.supportrouter.model.Customer;
import com.supportrouter.model.GeneralTicket;
import com.supportrouter.model.Priority;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.TechnicalTicket;
import com.supportrouter.model.Ticket;
import com.supportrouter.observer.TicketObserver;
import com.supportrouter.service.SupportRouter;
import com.supportrouter.state.ClosedState;
import com.supportrouter.state.ResolvedState;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupportRouterTest {
    private final Customer customer = new Customer("C-1", "Ada", "ada@example.com");

    @Test
    void routesEveryCategoryToTheCorrectTicketType() {
        SupportRouter router = new SupportRouter();

        assertInstanceOf(BillingTicket.class, router.route(request("payment failed")));
        assertInstanceOf(TechnicalTicket.class, router.route(request("application bug")));
        assertInstanceOf(AccountTicket.class, router.route(request("account locked")));
        assertInstanceOf(GeneralTicket.class, router.route(request("general question")));
    }

    @Test
    void ticketStartsOpenAndRequiresResolutionBeforeClosing() {
        Ticket ticket = new SupportRouter().route(request("general request"));

        ticket.assign();
        ticket.start();
        assertThrows(IllegalStateException.class, ticket::close);

        ticket.resolve();
        assertInstanceOf(ResolvedState.class, ticket.getState());
        ticket.close();
        assertInstanceOf(ClosedState.class, ticket.getState());
    }

    @Test
    void stateChangesNotifyObservers() {
        Ticket ticket = new SupportRouter().route(request("account request"));
        List<String> changes = new ArrayList<>();
        TicketObserver observer = (updatedTicket, oldState, newState) -> changes
                .add(oldState.getClass().getSimpleName() + "->" + newState.getClass().getSimpleName());
        ticket.addObserver(observer);

        ticket.assign();

        assertEquals(List.of("OpenState->AssignedState"), changes);
    }

    @Test
    void strategiesHandleClassificationAndCriticalPriority() {
        assertEquals(Category.BILLING,
                new KeywordClassificationStrategy().classify(
                        new SupportRequest("R-2", customer, "payment failed")));
        assertEquals(Priority.CRITICAL, new RuleBasedPriorityStrategy().determinePriority(
                new SupportRequest("R-3", customer, "security outage")));
    }

    private SupportRequest request(String message) {
        return new SupportRequest("R-1", customer, message);
    }
}
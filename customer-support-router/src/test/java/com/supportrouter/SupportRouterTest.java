package com.supportrouter;

import com.supportrouter.model.AccountTicket;
import com.supportrouter.model.BillingTicket;
import com.supportrouter.model.Category;
import com.supportrouter.model.Customer;
import com.supportrouter.model.GeneralTicket;
import com.supportrouter.model.Priority;
import com.supportrouter.model.StatusChange;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.TechnicalTicket;
import com.supportrouter.model.Ticket;
import com.supportrouter.observer.TicketObserver;
import com.supportrouter.repository.InMemoryTicketRepository;
import com.supportrouter.service.SupportRouter;
import com.supportrouter.state.AssignedState;
import com.supportrouter.state.ClosedState;
import com.supportrouter.state.InProgressState;
import com.supportrouter.state.OpenState;
import com.supportrouter.state.ResolvedState;
import com.supportrouter.state.TicketState;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportRouterTest {
    private final Customer customer = new Customer("C-1", "Ada", "ada@example.com");
    private int nextRequestId = 1;

    @Test
    void routesEveryCategoryToTheCorrectTicketType() {
        SupportRouter router = new SupportRouter();

        assertInstanceOf(BillingTicket.class, router.route(request("payment failed")));
        assertInstanceOf(TechnicalTicket.class, router.route(request("application bug")));
        assertInstanceOf(AccountTicket.class, router.route(request("account locked")));
        assertInstanceOf(GeneralTicket.class, router.route(request("general question")));
    }

    @Test
    void ticketFollowsFullLifecycle() {
        Ticket ticket = new SupportRouter().route(request("general request"));

        assertEquals("OPEN", ticket.getStatusName());
        ticket.assign();
        assertInstanceOf(AssignedState.class, ticket.getState());
        ticket.start();
        assertInstanceOf(InProgressState.class, ticket.getState());
        ticket.resolve();
        assertInstanceOf(ResolvedState.class, ticket.getState());
        ticket.close();
        assertInstanceOf(ClosedState.class, ticket.getState());
    }

    @Test
    void everyInvalidTransitionThrows() {
        assertInvalid(new OpenState(), TicketState::start, TicketState::resolve, TicketState::close);
        assertInvalid(new AssignedState(), TicketState::assign, TicketState::resolve, TicketState::close);
        assertInvalid(new InProgressState(), TicketState::assign, TicketState::start, TicketState::close);
        assertInvalid(new ResolvedState(), TicketState::assign, TicketState::start, TicketState::resolve);
        assertInvalid(new ClosedState(), TicketState::assign, TicketState::start, TicketState::resolve,
                TicketState::close);
    }

    @Test
    void invalidTransitionLeavesTicketUnchanged() {
        Ticket ticket = new SupportRouter().route(request("general request"));
        ticket.assign();

        assertThrows(IllegalStateException.class, ticket::close);
        assertEquals("ASSIGNED", ticket.getStatusName());
    }

    @Test
    void stateChangesNotifyObservers() {
        Ticket ticket = new SupportRouter().route(request("account request"));
        List<String> changes = new ArrayList<>();
        TicketObserver observer = (updatedTicket, oldState, newState) -> changes
                .add(oldState.getName() + "->" + newState.getName());
        ticket.addObserver(observer);

        ticket.assign();

        assertEquals(List.of("OPEN->ASSIGNED"), changes);
    }

    @Test
    void statusChangesAreRecordedInHistory() {
        SupportRouter router = new SupportRouter(new KeywordClassificationStrategy(),
                new RuleBasedPriorityStrategy(), new InMemoryTicketRepository());
        Ticket ticket = router.route(request("payment failed"));

        ticket.assign();
        ticket.start();

        List<StatusChange> history = router.getStatusHistory(ticket.getTicketId());
        assertEquals(2, history.size());
        assertEquals("OPEN", history.get(0).oldStatus());
        assertEquals("ASSIGNED", history.get(0).newStatus());
        assertEquals("INPROGRESS", history.get(1).newStatus());
    }

    @Test
    void ticketsLoadedThroughRouterStillPersistChanges() {
        SupportRouter router = new SupportRouter();
        String ticketId = router.route(request("account locked")).getTicketId();

        Ticket loaded = router.findTicket(ticketId).orElseThrow();
        loaded.assign();

        assertEquals(1, router.getStatusHistory(ticketId).size());
        assertEquals("ASSIGNED", router.findTicket(ticketId).orElseThrow().getStatusName());
    }

    @Test
    void statusNamesRoundTrip() {
        for (String name : List.of("OPEN", "ASSIGNED", "INPROGRESS", "RESOLVED", "CLOSED")) {
            assertEquals(name, TicketState.fromName(name).getName());
        }
        assertThrows(IllegalArgumentException.class, () -> TicketState.fromName("UNKNOWN"));
    }

    @Test
    void strategiesHandleClassificationAndCriticalPriority() {
        assertEquals(Category.BILLING,
                new KeywordClassificationStrategy().classify(
                        new SupportRequest("R-2", customer, "payment failed")));
        assertEquals(Priority.CRITICAL, new RuleBasedPriorityStrategy().determinePriority(
                new SupportRequest("R-3", customer, "security outage")));
    }

    @Test
    void createsSupportRequestFromClientDetails() {
        SupportRequest request = Main.createSupportRequest("C-123", "Ada", "ada@example.com", "Unable to login");

        assertEquals("C-123", request.getCustomer().getCustomerId());
        assertEquals("Ada", request.getCustomer().getName());
        assertEquals("ada@example.com", request.getCustomer().getEmail());
        assertEquals("Unable to login", request.getMessage());
        assertTrue(request.getRequestId().startsWith("R-"));
    }

    @Test
    void consoleCreatesListsUpdatesAndShowsHistory() {
        String input = String.join("\n",
                "1", "", "Ada", "ada@example.com", "payment failed",
                "2",
                "4", "nope",
                "4", "R-THIS-WILL-NOT-EXIST", "",
                "5") + "\n";
        SupportRouter router = new SupportRouter();
        String output = runConsole(input, router);
        String ticketId = router.listTickets().get(0).getTicketId();

        assertTrue(output.contains("Ticket created successfully"));
        assertTrue(output.contains("BILLING"));
        assertTrue(output.contains("No ticket found with ID nope."));

        String flow = String.join("\n",
                "4", ticketId, "c",
                "4", ticketId, "a",
                "3", ticketId,
                "5") + "\n";
        output = runConsole(flow, router);

        assertTrue(output.contains("Error: Cannot close an open ticket."));
        assertTrue(output.contains("Audit Log: Ticket " + ticketId + " changed from OPEN to ASSIGNED"));
        assertTrue(output.contains("Ticket " + ticketId + " is now ASSIGNED."));
        assertTrue(output.contains("OPEN -> ASSIGNED"));
    }

    private static String runConsole(String input, SupportRouter router) {
        java.io.PrintStream originalOut = System.out;
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(buffer, true));
        try {
            Main.runInteractiveConsole(new java.util.Scanner(input), router);
        } finally {
            System.setOut(originalOut);
        }
        return buffer.toString();
    }

    @SafeVarargs
    private static void assertInvalid(TicketState state, Function<TicketState, TicketState>... transitions) {
        for (Function<TicketState, TicketState> transition : transitions) {
            assertThrows(IllegalStateException.class, () -> transition.apply(state),
                    state.getName() + " should reject this transition");
        }
    }

    private SupportRequest request(String message) {
        return new SupportRequest("R-" + nextRequestId++, customer, message);
    }
}

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
import com.supportrouter.model.TicketAction;
import com.supportrouter.observer.TicketObserver;
import com.supportrouter.repository.InMemoryUserRepository;
import com.supportrouter.service.NotFoundException;
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

        assertInstanceOf(BillingTicket.class, router.createTicket(request("payment failed")));
        assertInstanceOf(TechnicalTicket.class, router.createTicket(request("application bug")));
        assertInstanceOf(AccountTicket.class, router.createTicket(request("account locked")));
        assertInstanceOf(GeneralTicket.class, router.createTicket(request("general question")));
    }

    @Test
    void ticketFollowsFullLifecycle() {
        Ticket ticket = new SupportRouter().createTicket(request("general request"));

        assertEquals("OPEN", ticket.getStatusName());
        ticket.assign("ravi");
        assertInstanceOf(AssignedState.class, ticket.getState());
        assertEquals("ravi", ticket.getAssignedAgent());
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
        Ticket ticket = new SupportRouter().createTicket(request("general request"));
        ticket.assign("ravi");

        assertThrows(IllegalStateException.class, ticket::close);
        assertThrows(IllegalStateException.class, () -> ticket.assign("meena"));
        assertEquals("ASSIGNED", ticket.getStatusName());
        assertEquals("ravi", ticket.getAssignedAgent());
    }

    @Test
    void stateChangesNotifyObservers() {
        Ticket ticket = new SupportRouter().createTicket(request("account request"));
        List<String> changes = new ArrayList<>();
        TicketObserver observer = (updatedTicket, oldState, newState) -> changes
                .add(oldState.getName() + "->" + newState.getName());
        ticket.addObserver(observer);

        ticket.assign("ravi");

        assertEquals(List.of("OPEN->ASSIGNED"), changes);
    }

    @Test
    void routerAssignsAndChangesStatusWithHistory() {
        SupportRouter router = new SupportRouter();
        String id = router.createTicket(request("payment failed")).getTicketId();

        router.assignTicket(id, "ravi");
        router.changeStatus(id, TicketAction.START);

        Ticket reloaded = router.getTicket(id);
        assertEquals("INPROGRESS", reloaded.getStatusName());
        assertEquals("ravi", reloaded.getAssignedAgent());
        List<StatusChange> history = router.getStatusHistory(id);
        assertEquals(2, history.size());
        assertEquals("OPEN", history.get(0).oldStatus());
        assertEquals("ASSIGNED", history.get(0).newStatus());
        assertEquals("INPROGRESS", history.get(1).newStatus());
    }

    @Test
    void assigningRequiresARealAgent() {
        SupportRouter router = new SupportRouter();
        String id = router.createTicket(request("payment failed")).getTicketId();

        assertThrows(IllegalArgumentException.class, () -> router.assignTicket(id, "nobody"));
        assertThrows(IllegalArgumentException.class, () -> router.assignTicket(id, "sanjay"));
        assertEquals("OPEN", router.getTicket(id).getStatusName());
    }

    @Test
    void unknownTicketThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> new SupportRouter().getTicket("missing"));
    }

    @Test
    void statusNamesRoundTrip() {
        for (String name : List.of("OPEN", "ASSIGNED", "INPROGRESS", "RESOLVED", "CLOSED")) {
            assertEquals(name, TicketState.fromName(name).getName());
        }
        assertThrows(IllegalArgumentException.class, () -> TicketState.fromName("UNKNOWN"));
    }

    @Test
    void ticketActionParsesInputCaseInsensitively() {
        assertEquals(TicketAction.RESOLVE, TicketAction.parse(" resolve "));
        assertThrows(IllegalArgumentException.class, () -> TicketAction.parse("delete"));
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
    void consoleEnforcesRolesAcrossLogins() {
        SupportRouter router = new SupportRouter();
        InMemoryUserRepository users = InMemoryUserRepository.withDemoUsers();

        String output = runConsole(String.join("\n",
                "nobody",
                "priya", "1", "payment failed", "4", "6",
                "sanjay", "2", "0") + "\n", router, users);

        assertTrue(output.contains("Unknown user: nobody"));
        assertTrue(output.contains("Welcome, Priya Sharma (CUSTOMER)"));
        assertTrue(output.contains("Ticket created successfully"));
        assertTrue(output.contains("Error: CUSTOMER 'priya' is not allowed to assign ticket."));
        assertTrue(output.contains("Welcome, Sanjay Rao (SUPERVISOR)"));
        String ticketId = router.listTickets().get(0).getTicketId();

        output = runConsole(String.join("\n",
                "sanjay", "5", ticketId, "close",
                "4", ticketId, "ravi", "6",
                "ravi", "5", ticketId, "start",
                "3", ticketId, "0") + "\n", router, users);

        assertTrue(output.contains("Error: Cannot close an open ticket."));
        assertTrue(output.contains("is now ASSIGNED and assigned to ravi"));
        assertTrue(output.contains("Audit Log: Ticket " + ticketId + " changed from ASSIGNED to INPROGRESS"));
        assertTrue(output.contains("OPEN -> ASSIGNED"));
        assertTrue(output.contains("ASSIGNED -> INPROGRESS"));
    }

    private static String runConsole(String input, SupportRouter router, InMemoryUserRepository users) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true));
        try {
            Main.runInteractiveConsole(new Scanner(input), router, users);
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

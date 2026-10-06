package com.supportrouter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.supportrouter.model.Customer;
import com.supportrouter.model.Permission;
import com.supportrouter.model.Role;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.model.TicketAction;
import com.supportrouter.model.User;
import com.supportrouter.repository.InMemoryUserRepository;
import com.supportrouter.service.AccessDeniedException;
import com.supportrouter.service.SecuredSupportRouter;
import com.supportrouter.service.SupportRouter;

class SecuredSupportRouterTest {
    private final InMemoryUserRepository users = InMemoryUserRepository.withDemoUsers();
    private SupportRouter router;
    private SecuredSupportRouter priya;
    private SecuredSupportRouter arjun;
    private SecuredSupportRouter ravi;
    private SecuredSupportRouter meena;
    private SecuredSupportRouter sanjay;

    @BeforeEach
    void setUp() {
        router = new SupportRouter();
        priya = as("priya");
        arjun = as("arjun");
        ravi = as("ravi");
        meena = as("meena");
        sanjay = as("sanjay");
    }

    @Test
    void rolesHaveExpectedPermissions() {
        assertTrue(Role.CUSTOMER.allows(Permission.CREATE_TICKET));
        assertFalse(Role.CUSTOMER.allows(Permission.ASSIGN_TICKET));
        assertFalse(Role.CUSTOMER.allows(Permission.START_TICKET));
        assertFalse(Role.AGENT.allows(Permission.CREATE_TICKET));
        assertFalse(Role.AGENT.allows(Permission.CLOSE_TICKET));
        assertTrue(Role.AGENT.allows(Permission.RESOLVE_TICKET));
        for (Permission permission : List.of(Permission.CREATE_TICKET, Permission.VIEW_ALL_TICKETS,
                Permission.ASSIGN_TICKET, Permission.START_TICKET, Permission.RESOLVE_TICKET,
                Permission.CLOSE_TICKET)) {
            assertTrue(Role.SUPERVISOR.allows(permission));
        }
    }

    @Test
    void customerCreatesAndSeesOnlyOwnTickets() {
        Ticket mine = priya.createTicket(requestFor(customerOf("priya"), "payment failed"));
        arjun.createTicket(requestFor(customerOf("arjun"), "account locked"));

        assertEquals(List.of(mine.getTicketId()), ids(priya.listTickets()));
        assertEquals(mine.getTicketId(), priya.getTicket(mine.getTicketId()).getTicketId());
        assertThrows(AccessDeniedException.class, () -> arjun.getTicket(mine.getTicketId()));
        assertThrows(AccessDeniedException.class, () -> arjun.getStatusHistory(mine.getTicketId()));
        assertThrows(AccessDeniedException.class, () -> arjun.listTicketsForCustomer("priya"));
    }

    @Test
    void customerCannotCreateTicketForSomeoneElse() {
        assertThrows(AccessDeniedException.class,
                () -> priya.createTicket(requestFor(customerOf("arjun"), "payment failed")));
        assertTrue(router.listTickets().isEmpty());
    }

    @Test
    void customerCannotAssignOrChangeStatus() {
        String id = priya.createTicket(requestFor(customerOf("priya"), "payment failed")).getTicketId();

        assertThrows(AccessDeniedException.class, () -> priya.assignTicket(id, "ravi"));
        assertThrows(AccessDeniedException.class, () -> priya.changeStatus(id, TicketAction.CLOSE));
        assertThrows(AccessDeniedException.class, () -> priya.listAgents());
    }

    @Test
    void agentCannotCreateOrAssign() {
        assertThrows(AccessDeniedException.class,
                () -> ravi.createTicket(requestFor(customerOf("ravi"), "payment failed")));
        String id = ticketFromPriya();
        assertThrows(AccessDeniedException.class, () -> ravi.assignTicket(id, "ravi"));
    }

    @Test
    void agentOnlySeesAndWorksOnAssignedTickets() {
        String forRavi = ticketFromPriya();
        String forMeena = ticketFromPriya();
        sanjay.assignTicket(forRavi, "ravi");
        sanjay.assignTicket(forMeena, "meena");

        assertEquals(List.of(forRavi), ids(ravi.listTickets()));
        assertThrows(AccessDeniedException.class, () -> ravi.getTicket(forMeena));
        assertThrows(AccessDeniedException.class, () -> ravi.changeStatus(forMeena, TicketAction.START));
        assertEquals("ASSIGNED", router.getTicket(forMeena).getStatusName());

        ravi.changeStatus(forRavi, TicketAction.START);
        ravi.changeStatus(forRavi, TicketAction.RESOLVE);
        assertEquals("RESOLVED", router.getTicket(forRavi).getStatusName());
        assertThrows(AccessDeniedException.class, () -> ravi.changeStatus(forRavi, TicketAction.CLOSE));
        assertEquals("RESOLVED", router.getTicket(forRavi).getStatusName());
        assertTrue(meena.listTickets().stream().noneMatch(t -> t.getTicketId().equals(forRavi)));
    }

    @Test
    void supervisorSeesEverythingAndCanCloseWithHistory() {
        String id = ticketFromPriya();
        arjun.createTicket(requestFor(customerOf("arjun"), "general question"));

        assertEquals(2, sanjay.listTickets().size());
        sanjay.assignTicket(id, "meena");
        sanjay.changeStatus(id, TicketAction.START);
        sanjay.changeStatus(id, TicketAction.RESOLVE);
        sanjay.changeStatus(id, TicketAction.CLOSE);

        assertEquals("CLOSED", router.getTicket(id).getStatusName());
        assertEquals(4, sanjay.getStatusHistory(id).size());
        assertEquals(List.of("meena", "ravi"), sanjay.listAgents().stream().map(User::username).sorted().toList());
    }

    @Test
    void supervisorCanCreateTicketForAnyCustomer() {
        Ticket ticket = sanjay.createTicket(requestFor(new Customer("C-99", "Walk-in", "walkin@example.com"),
                "billing question"));
        assertEquals("C-99", ticket.getSupportRequest().getCustomer().getCustomerId());
    }

    private String ticketFromPriya() {
        return priya.createTicket(requestFor(customerOf("priya"), "payment failed")).getTicketId();
    }

    private SecuredSupportRouter as(String username) {
        return new SecuredSupportRouter(router, users.findByUsername(username).orElseThrow());
    }

    private Customer customerOf(String username) {
        return users.findByUsername(username).orElseThrow().asCustomer();
    }

    private static SupportRequest requestFor(Customer customer, String message) {
        return new SupportRequest("R-" + UUID.randomUUID(), customer, message);
    }

    private static List<String> ids(List<Ticket> tickets) {
        return tickets.stream().map(Ticket::getTicketId).toList();
    }
}

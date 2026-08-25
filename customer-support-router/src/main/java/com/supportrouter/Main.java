package com.supportrouter;

import com.supportrouter.model.Customer;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;
import com.supportrouter.service.SupportRouter;

public class Main {
    public static void main(String[] args) {
        Customer customer = new Customer("C-1", "Ada Lovelace", "ada@example.com");
        SupportRequest request = new SupportRequest(
                "R-1",
                customer,
                "I am having trouble logging into my account. Please assist.,account locked");

        Ticket ticket = new SupportRouter().route(request);
        ticket.assign();
        ticket.start();
        ticket.resolve();
        ticket.close();
    }
}
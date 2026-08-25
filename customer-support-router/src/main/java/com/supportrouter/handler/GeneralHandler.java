package com.supportrouter.handler;


import com.supportrouter.factory.TicketFactory;
import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Ticket;

public class GeneralHandler extends SupportHandler {
    @Override
    public Ticket handle(SupportRequest request){
        if(request.getCategory() == Category.GENERAL){
            return TicketFactory.createTicket(request);
        }
        return next == null?null:next.handle(request);
    }
}


package com.supportrouter.factory;

import com.supportrouter.model.AccountTicket;
import com.supportrouter.model.BillingTicket;
import com.supportrouter.model.GeneralTicket;
import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.TechnicalTicket;
import com.supportrouter.model.Ticket;

public class TicketFactory {
    public static Ticket createTicket(SupportRequest request){
      return switch(request.getCategory()){
        case BILLING -> new BillingTicket(request);
        case ACCOUNT -> new AccountTicket(request);
        case TECHNICAL -> new TechnicalTicket(request);
        case GENERAL -> new GeneralTicket(request);
      };
    } 
}

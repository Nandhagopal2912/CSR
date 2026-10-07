package com.supportrouter.handler;

import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

public class BillingHandler extends SupportHandler {
    @Override
    protected boolean canHandle(SupportRequest request) {
        return request.getCategory() == Category.BILLING;
    }
}

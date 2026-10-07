package com.supportrouter.handler;

import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

public class AccountHandler extends SupportHandler {
    @Override
    protected boolean canHandle(SupportRequest request) {
        return request.getCategory() == Category.ACCOUNT;
    }
}

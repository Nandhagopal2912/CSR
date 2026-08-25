package com.supportrouter.strategy;

import com.supportrouter.model.SupportRequest;
import com.supportrouter.model.Priority;

public interface PriorityStrategy {
    Priority determinePriority(SupportRequest request);
}

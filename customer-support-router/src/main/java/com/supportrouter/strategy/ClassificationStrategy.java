package com.supportrouter.strategy;

import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

public interface ClassificationStrategy {
    Category classify(SupportRequest request);
}

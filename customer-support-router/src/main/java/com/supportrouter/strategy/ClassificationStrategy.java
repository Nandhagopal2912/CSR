package com.supportrouter.strategy;

import com.supportrouter.model.Category;
import com.supportrouter.model.SupportRequest;

public interface ClassificationStrategy {
public Category classify(SupportRequest request);
}

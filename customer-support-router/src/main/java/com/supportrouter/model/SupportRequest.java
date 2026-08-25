package com.supportrouter.model;

public class SupportRequest {
    private String requestId;
    private Customer customer;
    private String message;
    private Category category;
    private Priority priority;

    public SupportRequest(String requestId, Customer customer, String message) {
        validateBaseFields(requestId, customer, message);
        this.requestId = requestId;
        this.customer = customer;
        this.message = message;
    }

    public SupportRequest(String requestId, Customer customer, String message, Category category, Priority priority) {
        this(requestId, customer, message);
        if (category == null || priority == null) {
            throw new IllegalArgumentException("category and priority are required");
        }
        this.category = category;
        this.priority = priority;
    }

    public SupportRequest withClassification(Category category, Priority priority) {
        return new SupportRequest(requestId, customer, message, category, priority);
    }

    private static void validateBaseFields(String requestId, Customer customer, String message) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (customer == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("customer and message are required");
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getMessage() {
        return message;
    }

    public Category getCategory() {
        return category;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getAssignedTeam() {
        if (category == null) {
            throw new IllegalStateException("Category has not been assigned");
        }
        switch (category) {
            case BILLING:
                return "Billing Support Team";
            case TECHNICAL:
                return "Technical Support Team";
            case ACCOUNT:
                return "Account Management Team";
            default:
                return "General Support Team";
        }
    }

}

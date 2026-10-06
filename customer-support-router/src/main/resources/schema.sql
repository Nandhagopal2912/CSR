CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'AGENT', 'SUPERVISOR'))
);

CREATE TABLE tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(50) NOT NULL UNIQUE,
    customer_id VARCHAR(50) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    assigned_team VARCHAR(100) NOT NULL,
    assigned_agent VARCHAR(50) NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tickets_assigned_agent
        FOREIGN KEY (assigned_agent) REFERENCES users(username)
);

CREATE TABLE ticket_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_status_history_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE
);

INSERT INTO users (username, name, email, role) VALUES
    ('priya', 'Priya Sharma', 'priya@example.com', 'CUSTOMER'),
    ('arjun', 'Arjun Kumar', 'arjun@example.com', 'CUSTOMER'),
    ('ravi', 'Ravi Shankar', 'ravi@support.example.com', 'AGENT'),
    ('meena', 'Meena Iyer', 'meena@support.example.com', 'AGENT'),
    ('sanjay', 'Sanjay Rao', 'sanjay@support.example.com', 'SUPERVISOR');

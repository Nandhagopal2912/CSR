-- Upgrades a database created with the previous schema.sql. Existing tickets are kept.

CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'AGENT', 'SUPERVISOR'))
);

INSERT INTO users (username, name, email, role) VALUES
    ('priya', 'Priya Sharma', 'priya@example.com', 'CUSTOMER'),
    ('arjun', 'Arjun Kumar', 'arjun@example.com', 'CUSTOMER'),
    ('ravi', 'Ravi Shankar', 'ravi@support.example.com', 'AGENT'),
    ('meena', 'Meena Iyer', 'meena@support.example.com', 'AGENT'),
    ('sanjay', 'Sanjay Rao', 'sanjay@support.example.com', 'SUPERVISOR');

ALTER TABLE tickets
    ADD COLUMN assigned_agent VARCHAR(50) NULL AFTER assigned_team,
    ADD CONSTRAINT fk_tickets_assigned_agent FOREIGN KEY (assigned_agent) REFERENCES users(username);

ALTER TABLE ticket_status_history DROP FOREIGN KEY fk_ticket_status_history_ticket;

ALTER TABLE ticket_status_history
    ADD CONSTRAINT fk_ticket_status_history_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE;

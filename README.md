**1.Project Description ** The Customer Support Router is a software system that automatically processes incoming customer support requests, determines their category and priority, routes them to the appropriate support handler, creates a corresponding support ticket, and manages the ticket throughout its lifecycle. The primary objective of the project is to demonstrate how software design patterns can be applied to solve common problems in a real-world software system. Instead of requiring a customer support administrator to manually classify and route every request, the system automates the initial processing. Example: "My payment was deducted twice for the same order." The system can process the request as: Customer Request ↓ Request Classification ↓ Category: Billing ↓ Priority: High ↓ Billing Handler ↓ Billing Ticket Created ↓ Ticket Status: OPEN The system will be implemented as a manageable prototype rather than a complete commercial customer-support platform.


**2. Project Objectives ** The project has the following objectives:
Automatically process customer support requests.
Classify requests into predefined categories.
Determine request priority.
Route requests to appropriate support handlers.
Create category-specific support tickets.
Maintain the lifecycle of each ticket.
Notify interested components when ticket events occur.
Demonstrate practical applications of software design patterns.
Maintain low coupling between system components.
Make the system extensible for future categories and processing strategies.


# Customer Support Router

A Java 17 demonstration project for routing customer support requests to specialized ticket types. The project showcases several object-oriented design patterns working together:

- **Factory:** creates the correct ticket implementation for a category.
- **Strategy:** classifies requests and determines ticket priority.
- **State:** controls the ticket lifecycle from open to closed.
- **Observer:** publishes audit, notification, and dashboard updates when a ticket changes state.

## Requirements

- Java Development Kit (JDK) 17 or later
- Apache Maven 3.8 or later

Check your installation:

```bash
java -version
mvn -version
```

## Getting Started

Clone the repository and move into the Maven project directory:

```bash
git clone https://github.com/<your-username>/<your-repository>.git
cd <your-repository>/customer-support-router
```

Replace the repository URL and folder name with your GitHub repository details.

## Run the Application

Compile the project and run the `Main` class:

```bash
mvn package -DskipTests
java -cp target/classes com.supportrouter.Main
```

The application prints the routed ticket summary and shows each lifecycle transition, including observer notifications.

## Run Tests

Run the JUnit test suite with Maven:

```bash
mvn test
```

The tests cover category routing, ticket lifecycle rules, observer notifications, request classification, and critical-priority handling.

## Project Structure

```text
src/
├── main/java/com/supportrouter/
│   ├── factory/       Ticket creation
│   ├── handler/       Category-specific handling
│   ├── model/         Requests, customers, tickets, and enums
│   ├── observer/      Audit, notification, and dashboard observers
│   ├── service/       Support request routing
│   ├── state/         Ticket lifecycle states
│   └── strategy/      Classification and priority strategies
└── test/java/         JUnit tests
```

## Ticket Lifecycle

Tickets follow this lifecycle:

```text
Open -> Assigned -> In Progress -> Resolved -> Closed
```

Closing a ticket before it is resolved is rejected by the state machine.

# Customer Support Router

A Java 17 demonstration project for routing customer support requests to specialized ticket types. The project showcases several object-oriented design patterns working together:

- **Factory:** creates the correct ticket implementation for a category.
- **Strategy:** classifies requests and determines ticket priority.
- **State:** controls the ticket lifecycle from open to closed.
- **Observer:** publishes audit, notification, and dashboard updates when a ticket changes state.

## Dependencies and Requirements

- Java Development Kit (JDK) 17 or later
- Apache Maven 3.8 or later
- MySQL 8 or later, when using the MySQL repository

Maven downloads the Java dependencies automatically. The project uses:

- MySQL Connector/J for database access
- dotenv-java for loading local `.env` configuration
- JUnit Jupiter for tests
- Exec Maven Plugin for the `mvn compile exec:java` run command

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

## Configure the Database

The application uses MySQL persistence. Create a database, then run
`customer-support-router/src/main/resources/schema.sql` against it:

```sql
CREATE DATABASE customer_support;
```

Copy the configuration template and edit the local values:

```bash
copy .env.example .env
```

The `.env` file should contain:

```env
DB_URL=jdbc:mysql://localhost:3306/customer_support
DB_USER=root
DB_PASSWORD=your_mysql_password
```

`.env` is ignored by Git. Never commit passwords or other credentials. The
committed `.env.example` file contains placeholders only.

## Run the Application

From the `customer-support-router` directory, compile and run the `Main` class:

```bash
mvn compile exec:java
```

The database generates a unique numeric ticket ID for each new ticket. The
application prints the routed ticket summary and each lifecycle transition.

## MySQL Persistence

The application uses `MySqlTicketRepository` in `Main`. Other code can inject the
repository into `SupportRouter`:

```java
TicketRepository repository = new MySqlTicketRepository(
	"jdbc:mysql://localhost:3306/customer_support",
	System.getenv("DB_USER"),
	System.getenv("DB_PASSWORD"));

SupportRouter router = new SupportRouter(
	new KeywordClassificationStrategy(),
	new RuleBasedPriorityStrategy(),
	repository);
```

`TicketRepository` provides create, find-by-ID, find-all, update, and delete
operations. MySQL generates `ticket_id` as an auto-increment primary key, while
the caller's request identifier is stored separately in `request_id`. State
changes are persisted through `PersistenceObserver` and recorded in
`ticket_status_history`.

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

## Useful Commands

Run a clean build without tests:

```bash
mvn clean package -DskipTests
```

Run the application:

```bash
mvn compile exec:java
```

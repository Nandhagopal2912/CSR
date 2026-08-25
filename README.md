**1.Project Description ** The Customer Support Router is a software system that automatically processes incoming customer support requests, determines their category and priority, routes them to the appropriate support handler, creates a corresponding support ticket, and manages the ticket throughout its lifecycle. The primary objective of the project is to demonstrate how software design patterns can be applied to solve common problems in a real-world software system. Instead of requiring a customer support administrator to manually classify and route every request, the system automates the initial processing. Example: "My payment was deducted twice for the same order." The system can process the request as: Customer Request ↓ Request Classification ↓ Category: Billing ↓ Priority: High ↓ Billing Handler ↓ Billing Ticket Created ↓ Ticket Status: OPEN The system will be implemented as a manageable prototype rather than a complete commercial customer-support platform.

**3. Project Objectives ** The project has the following objectives:

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

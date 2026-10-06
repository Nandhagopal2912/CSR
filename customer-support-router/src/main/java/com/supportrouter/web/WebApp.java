package com.supportrouter.web;

import com.supportrouter.Application;
import com.supportrouter.repository.DataAccessException;
import com.supportrouter.service.AccessDeniedException;
import com.supportrouter.service.NotFoundException;
import com.supportrouter.web.ApiModels.ErrorResponse;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

public class WebApp {
    public static void main(String[] args) {
        Application app;
        try {
            app = Application.fromArgs(args);
        } catch (RuntimeException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
            return;
        }

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        create(app).start(port);
        System.out.println("Customer Support Router is running at http://localhost:" + port);
    }

    public static Javalin create(Application app) {
        TicketController controller = new TicketController(app.service(), app.users());

        Javalin javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.staticFiles.add("/public", Location.CLASSPATH);
        });

        javalin.post("/api/login", controller::login);
        javalin.post("/api/logout", controller::logout);
        javalin.get("/api/me", controller::me);
        javalin.get("/api/tickets", controller::listTickets);
        javalin.post("/api/tickets", controller::createTicket);
        javalin.get("/api/tickets/{id}", controller::getTicket);
        javalin.post("/api/tickets/{id}/assign", controller::assignTicket);
        javalin.post("/api/tickets/{id}/status", controller::changeStatus);
        javalin.get("/api/agents", controller::listAgents);

        javalin.exception(NotAuthenticatedException.class, (e, ctx) -> error(ctx, 401, e.getMessage()));
        javalin.exception(AccessDeniedException.class, (e, ctx) -> error(ctx, 403, e.getMessage()));
        javalin.exception(NotFoundException.class, (e, ctx) -> error(ctx, 404, e.getMessage()));
        javalin.exception(IllegalArgumentException.class, (e, ctx) -> error(ctx, 400, e.getMessage()));
        javalin.exception(IllegalStateException.class, (e, ctx) -> error(ctx, 409, e.getMessage()));
        javalin.exception(DataAccessException.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "Database error. Check the server console for details.");
        });

        return javalin;
    }

    private static void error(Context ctx, int status, String message) {
        ctx.status(status).json(new ErrorResponse(message));
    }
}

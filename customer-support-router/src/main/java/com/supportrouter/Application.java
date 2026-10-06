package com.supportrouter;

import java.util.Arrays;

import com.supportrouter.repository.DatabaseConfig;
import com.supportrouter.repository.InMemoryTicketRepository;
import com.supportrouter.repository.InMemoryUserRepository;
import com.supportrouter.repository.MySqlTicketRepository;
import com.supportrouter.repository.MySqlUserRepository;
import com.supportrouter.repository.TicketRepository;
import com.supportrouter.repository.UserRepository;
import com.supportrouter.service.SupportRouter;
import com.supportrouter.service.SupportService;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

public record Application(SupportService service, UserRepository users) {
    public static final String IN_MEMORY_FLAG = "--in-memory";

    public static Application fromArgs(String[] args) {
        if (Arrays.asList(args).contains(IN_MEMORY_FLAG)) {
            System.out.println("Using in-memory storage with demo users (nothing is saved).");
            return create(new InMemoryTicketRepository(), InMemoryUserRepository.withDemoUsers());
        }
        DatabaseConfig config = DatabaseConfig.fromEnvironment();
        config.checkConnection();
        return create(new MySqlTicketRepository(config), new MySqlUserRepository(config));
    }

    private static Application create(TicketRepository tickets, UserRepository users) {
        SupportRouter router = new SupportRouter(new KeywordClassificationStrategy(),
                new RuleBasedPriorityStrategy(), tickets, users);
        return new Application(router, users);
    }
}

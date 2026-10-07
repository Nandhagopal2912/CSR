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
import com.supportrouter.strategy.ClassificationStrategy;
import com.supportrouter.strategy.KeywordClassificationStrategy;
import com.supportrouter.strategy.LlmClassificationStrategy;
import com.supportrouter.strategy.RuleBasedPriorityStrategy;

public record Application(SupportService service, UserRepository users) {
    public static final String IN_MEMORY_FLAG = "--in-memory";
    public static final String NO_LLM_FLAG = "--no-llm";

    public static Application fromArgs(String[] args) {
        boolean useLlm = !Arrays.asList(args).contains(NO_LLM_FLAG)
                && Boolean.parseBoolean(Settings.get("LLM_ENABLED", "true"));
        ClassificationStrategy classifier = classifier(useLlm);

        if (Arrays.asList(args).contains(IN_MEMORY_FLAG)) {
            System.out.println("Using in-memory storage with demo users (nothing is saved).");
            return create(classifier, new InMemoryTicketRepository(), InMemoryUserRepository.withDemoUsers());
        }
        DatabaseConfig config = DatabaseConfig.fromEnvironment();
        config.checkConnection();
        return create(classifier, new MySqlTicketRepository(config), new MySqlUserRepository(config));
    }

    private static ClassificationStrategy classifier(boolean useLlm) {
        if (!useLlm) {
            return new KeywordClassificationStrategy();
        }
        String url = Settings.get("OLLAMA_URL", "http://localhost:11434");
        String model = Settings.get("OLLAMA_MODEL", "qwen2.5:0.5b");
        System.out.println("Keyword classification falls back to LLM " + model + " at " + url
                + " (GENERAL if unavailable).");
        return new KeywordClassificationStrategy(new LlmClassificationStrategy(url, model));
    }

    private static Application create(ClassificationStrategy classifier, TicketRepository tickets,
            UserRepository users) {
        SupportRouter router = new SupportRouter(classifier, new RuleBasedPriorityStrategy(), tickets, users,
                SupportRouter.defaultObservers());
        return new Application(router, users);
    }
}

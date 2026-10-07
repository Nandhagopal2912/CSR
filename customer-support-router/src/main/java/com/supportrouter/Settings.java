package com.supportrouter;

import io.github.cdimascio.dotenv.Dotenv;

public final class Settings {
    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private Settings() {
    }

    public static String get(String name, String defaultValue) {
        String environmentValue = System.getenv(name);
        return environmentValue != null ? environmentValue : DOTENV.get(name, defaultValue);
    }
}

package com.trousseau.util;

import java.util.Optional;

/** Deployment settings that come from outside the WAR. */
public final class AppConfig {

    private AppConfig() {
    }

    /**
     * The address users reach the app at, without a trailing slash, for example
     * {@code https://wardrobe.example.com/trousseau}. Used to build links in emails.
     *
     * <p>Read from the {@code trousseau.base-url} system property, then the
     * {@code TROUSSEAU_BASE_URL} environment variable. It is deliberately never derived
     * from the current request: the Host header is chosen by the client, so a password
     * reset requested with a forged Host would email the victim a link to the attacker's
     * server, carrying a valid token.</p>
     */
    public static Optional<String> baseUrl() {
        String value = System.getProperty("trousseau.base-url");
        if (value == null || value.isBlank()) {
            value = System.getenv("TROUSSEAU_BASE_URL");
        }
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        value = value.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return Optional.of(value);
    }
}

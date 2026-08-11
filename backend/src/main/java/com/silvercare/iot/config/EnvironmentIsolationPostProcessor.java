package com.silvercare.iot.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.util.Locale;

public class EnvironmentIsolationPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean development = environment.acceptsProfiles(Profiles.of("dev"));
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        if (development && production) {
            throw new IllegalStateException("The dev and prod Spring profiles cannot be active together");
        }

        String jdbcUrl = environment.getProperty("spring.datasource.url");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }
        String database = databaseName(jdbcUrl);
        boolean developmentDatabase = database.toLowerCase(Locale.ROOT).endsWith("_dev");
        if (production && developmentDatabase) {
            throw new IllegalStateException("The prod profile cannot connect to a database ending in _dev");
        }
        if (!production && !developmentDatabase) {
            throw new IllegalStateException("The dev profile requires a database name ending in _dev");
        }
    }

    private String databaseName(String jdbcUrl) {
        String withoutQuery = jdbcUrl.split("\\?", 2)[0];
        int slash = withoutQuery.lastIndexOf('/');
        if (slash < 0 || slash == withoutQuery.length() - 1) {
            throw new IllegalStateException("The datasource URL must contain an explicit database name");
        }
        return withoutQuery.substring(slash + 1);
    }
}

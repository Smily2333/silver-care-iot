package com.silvercare.iot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentIsolationPostProcessorTest {
    private final EnvironmentIsolationPostProcessor processor = new EnvironmentIsolationPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    void allowsDevelopmentDatabaseForDevelopmentProfile() {
        MockEnvironment environment = environment("dev", "jdbc:mysql://127.0.0.1:3306/silver_care_dev");
        assertDoesNotThrow(() -> processor.postProcessEnvironment(environment, application));
    }

    @Test
    void rejectsProductionDatabaseForDevelopmentProfile() {
        MockEnvironment environment = environment("dev", "jdbc:mysql://127.0.0.1:3306/silver_care");
        assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(environment, application));
    }

    @Test
    void rejectsDevelopmentDatabaseForProductionProfile() {
        MockEnvironment environment = environment("prod", "jdbc:mysql://127.0.0.1:3306/silver_care_dev");
        assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(environment, application));
    }

    @Test
    void rejectsActivatingDevelopmentAndProductionTogether() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/silver_care");
        environment.setActiveProfiles("dev", "prod");
        assertThrows(IllegalStateException.class,
                () -> processor.postProcessEnvironment(environment, application));
    }

    private MockEnvironment environment(String profile, String url) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.datasource.url", url);
        environment.setActiveProfiles(profile);
        return environment;
    }
}

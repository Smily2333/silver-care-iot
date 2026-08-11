package com.silvercare.iot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentProfileConfigTest {
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void defaultsToDevelopmentProfile() throws IOException {
        assertEquals("dev", property("application.yml", "spring.profiles.default"));
    }

    @Test
    void developmentUsesSeparateDatabaseAndSafeDeviceDefaults() throws IOException {
        String url = String.valueOf(property("application-dev.yml", "spring.datasource.url"));
        assertTrue(url.contains("silver_care_dev"));
        assertEquals("update", property("application-dev.yml", "spring.jpa.hibernate.ddl-auto"));
        assertEquals("${SILVER_CARE_DEV_GATEWAY_ENABLED:false}",
                property("application-dev.yml", "silver-care.gateway.enabled"));
        assertEquals("${SILVER_CARE_DEV_CONFIRMED_DEVICE_ACTIONS:}",
                property("application-dev.yml", "silver-care.device-actions.confirmed-types"));
    }

    @Test
    void productionRequiresDedicatedCredentialsAndValidatesSchema() throws IOException {
        assertEquals("${SILVER_CARE_PROD_DB_URL}",
                property("application-prod.yml", "spring.datasource.url"));
        assertEquals("${SILVER_CARE_PROD_ADMIN_PASSWORD}",
                property("application-prod.yml", "silver-care.admin.password"));
        assertEquals("validate", property("application-prod.yml", "spring.jpa.hibernate.ddl-auto"));
        assertFalse(String.valueOf(property("application-prod.yml", "spring.datasource.url"))
                .contains("silver_care_dev"));
    }

    private Object property(String resource, String key) throws IOException {
        List<PropertySource<?>> sources = loader.load(resource, new ClassPathResource(resource));
        return sources.stream()
                .map(source -> source.getProperty(key))
                .filter(value -> value != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing property " + key + " in " + resource));
    }
}

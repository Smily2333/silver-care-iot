package com.silvercare.iot.config;

import com.silvercare.iot.auth.MiniappTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class MiniappSecurityConfig {

    private final MiniappTokenAuthenticationFilter tokenAuthenticationFilter;

    public MiniappSecurityConfig(MiniappTokenAuthenticationFilter tokenAuthenticationFilter) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
    }

    @Bean
    @Order(1)
    SecurityFilterChain miniappSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/miniapp/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/miniapp/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/miniapp/**"))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, exception) -> response.sendError(401, "Authentication required");
    }
}

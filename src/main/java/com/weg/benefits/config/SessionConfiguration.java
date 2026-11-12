package com.weg.benefits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

@Configuration(proxyBeanMethods = false)
public class SessionConfiguration {
    @Bean
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> explicitSessionDeletion() {
        // Explicit logout/revocation must delete sessions even with a negative inactivity interval.
        // Expiry cleanup has its own query and keeps the configured no-expiration policy.
        return repository -> repository.setDeleteSessionQuery(
                "DELETE FROM %TABLE_NAME% WHERE SESSION_ID = ?");
    }
}

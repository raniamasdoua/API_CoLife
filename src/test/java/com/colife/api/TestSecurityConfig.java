package com.colife.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Configuration de test : fournit un {@link JwtDecoder} factice pour que le contexte du
 * resource server démarre sans appeler Keycloak. Les requêtes authentifiées des tests
 * injectent directement l'authentification via le post-processor {@code authentication(...)},
 * donc ce décodeur n'est jamais réellement invoqué.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                throw new JwtException("JwtDecoder factice : aucun décodage réel en test");
            }
        };
    }
}

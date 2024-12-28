package org.p2proto.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PreDestroy;

import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;

@Configuration
public class KeycloakAdminConfig {

    private final KeycloakProperties keycloakProperties;
    private Keycloak keycloak;

    public KeycloakAdminConfig(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    @Bean
    public Keycloak keycloak() {
        if (this.keycloak == null) {
            this.keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakProperties.getAuthorizationUri())
                    .realm(keycloakProperties.getRealm())
                    .clientId(keycloakProperties.getClientId())
                    .clientSecret(keycloakProperties.getClientSecret())
                    .grantType(CLIENT_CREDENTIALS)
                    .build();
        }
        return this.keycloak;
    }

    @PreDestroy
    public void shutdownKeycloak() {
        if (keycloak != null) {
            keycloak.close(); // Properly close resources on shutdown
        }
    }
}

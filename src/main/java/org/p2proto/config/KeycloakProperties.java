package org.p2proto.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class KeycloakProperties {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakProperties.class);

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.keycloak.realm}")
    private String realm;
    @Value("${spring.security.oauth2.client.registration.keycloak.redirect-uri}")
    private String redirectUri;
    @Value("${spring.security.oauth2.client.provider.keycloak.authorization-uri}")
    private String authorizationUri;
    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUri;
    @Value("${spring.security.oauth2.client.provider.keycloak.user-info-uri}")
    private String userInfoUri;
    @Value("${spring.security.oauth2.client.provider.keycloak.user-name-attribute}")
    private String userNameAttribute;
    @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}")
    private String jwkSetUri;
    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;
    @Value("${spring.security.oauth2.client.provider.keycloak.logout-uri}")
    private String keycloakLogoutUri;
    @Value("${spring.security.oauth2.client.provider.keycloak.post-logout-redirect-uri}")
    private String postLogoutRedirectUri;
}

package org.p2proto.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuth2ClientConfig {

    @Autowired
    private KeycloakProperties keycloakProperties;


    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration keycloakClientRegistration = keycloakClientRegistration();
        return new InMemoryClientRegistrationRepository(keycloakClientRegistration);
    }

    private ClientRegistration keycloakClientRegistration() {
        return ClientRegistration.withRegistrationId("keycloak")
            .clientId(keycloakProperties.getClientId())
            .clientSecret(keycloakProperties.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(keycloakProperties.getRedirectUri())
            .scope("openid", "profile", "email")
            .authorizationUri(keycloakProperties.getAuthorizationUri())
            .tokenUri(keycloakProperties.getTokenUri())
            .userInfoUri(keycloakProperties.getUserInfoUri())
            .userNameAttributeName(keycloakProperties.getUserNameAttribute())
            .jwkSetUri(keycloakProperties.getJwkSetUri())
            .issuerUri(keycloakProperties.getIssuerUri())
            .build();
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .clientCredentials()
                        .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }}

package org.p2proto.util;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class KeycloakAdminClient {

    private static Keycloak instance = null;

    private KeycloakAdminClient() {
    }

    public static Keycloak getInstance() {
        if (instance == null) {
            instance = KeycloakBuilder.builder()
                    .serverUrl("http://localhost:8080/auth")
                    .realm("master")
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId("admin-cli")
                    .clientSecret("YOUR_CLIENT_SECRET")
                    .build();
        }
        return instance;
    }
}

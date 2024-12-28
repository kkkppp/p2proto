package org.p2proto.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.p2proto.entity.ExtendedUser;
import org.p2proto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private String realm = "your_realm"; // Replace with your realm name

    public ExtendedUser getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<ExtendedUser> getAllUsers() {
        return userRepository.findAll();
    }

    private final Keycloak keycloak;

    @Autowired
    public UserService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public ExtendedUser saveUser(ExtendedUser user) {
        // Initialize Keycloak user representation
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setUsername(user.getUsername());
        keycloakUser.setEmail(user.getEmail());
        keycloakUser.setFirstName(user.getFirstName());
        keycloakUser.setLastName(user.getLastName());
        keycloakUser.setEnabled(true);

        // Create user in Keycloak
        Response response = keycloak.realm(realm).users().create(keycloakUser);
        if (response.getStatus() == 201) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            user.setId(userId);

            // Set password in Keycloak
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setTemporary(false);
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(user.getPassword());

                keycloak.realm(realm).users().get(userId).resetPassword(credential);
            }

            // Save user in system database (without password)
            return userRepository.save(user);
        } else {
            // Handle error
            String errorMessage = "Failed to create user in Keycloak: " + response.getStatusInfo().getReasonPhrase();
            throw new RuntimeException(errorMessage);
        }
    }

    public ExtendedUser updateUser(ExtendedUser user) {
        // Update user in Keycloak
        UserResource userResource = keycloak.realm(realm).users().get(user.getId());
        UserRepresentation keycloakUser = userResource.toRepresentation();

        keycloakUser.setEmail(user.getEmail());
        keycloakUser.setFirstName(user.getFirstName());
        keycloakUser.setLastName(user.getLastName());

        userResource.update(keycloakUser);

        // Update password if provided
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setTemporary(false);
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(user.getPassword());

            userResource.resetPassword(credential);
        }

        // Update user in system database
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        // Delete from Keycloak
        keycloak.realm(realm).users().delete(id);

        // Delete from system database
        userRepository.deleteById(id);
    }
}

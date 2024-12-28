package org.p2proto.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "extended_users")
public class ExtendedUser {

    @Id
    private String id; // Keycloak User ID

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    @Transient
    private String password;
    private String additionalInfo;
}

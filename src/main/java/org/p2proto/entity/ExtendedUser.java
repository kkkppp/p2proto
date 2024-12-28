package org.p2proto.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "users")
public class ExtendedUser {

    @Id
    private String id; // Keycloak User ID

    private String username;
    private String email;
    @Column(name="first_name")
    private String firstName;
    @Column(name="last_name")
    private String lastName;
    @Transient
    @Column(name="password_hash")
    private String password;
    //private String additionalInfo;
}

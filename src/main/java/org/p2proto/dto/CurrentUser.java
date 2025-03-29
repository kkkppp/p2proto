package org.p2proto.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CurrentUser {
    private Integer id;
    private UUID uuid;
    private String login;
    private String fullName;
    private String email;

    public CurrentUser(Integer id, UUID uuid, String login, String fullName, String email) {
        this.id = id;
        this.uuid = uuid;
        this.login = login;
        this.fullName = fullName;
        this.email = email;
    }
}

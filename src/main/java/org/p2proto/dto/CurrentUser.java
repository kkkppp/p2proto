package org.p2proto.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CurrentUser {
    private Integer currentUserId; // to avoid form binding conflicts
    private UUID uuid;
    private String login;
    private String fullName;
    private String email;

    public CurrentUser(Integer currentUserId, UUID uuid, String login, String fullName, String email) {
        this.currentUserId = currentUserId;
        this.uuid = uuid;
        this.login = login;
        this.fullName = fullName;
        this.email = email;
    }
}

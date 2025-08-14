package com.talkflow.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserData {
    private Long userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String profileUrl;
    private Boolean canChangePassword;
    private Boolean chatsExist;
}

package com.talkflow.dto.auth;

import lombok.*;

@Data
@AllArgsConstructor
@Builder
public class AuthenticationResponse {

    private String token;
    private UserData userData;

}

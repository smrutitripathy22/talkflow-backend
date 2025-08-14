package com.talkflow.dto.auth;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String token;
    private String password;
}

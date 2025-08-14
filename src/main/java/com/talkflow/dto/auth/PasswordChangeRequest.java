package com.talkflow.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordChangeRequest {

    @NotBlank(message = "Current Password is required")
    private String password;
    @NotBlank(message = "New Password is required")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,20}$",
            message = "Password must be 8-20 characters, include uppercase, lowercase, number, and special character"
    )
    private String updatePassword;
}

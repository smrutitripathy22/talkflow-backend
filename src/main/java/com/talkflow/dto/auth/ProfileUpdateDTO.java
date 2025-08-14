package com.talkflow.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfileUpdateDTO {
    @NotBlank
    @NotNull
    private String firstName;
    private String middleName;
    @NotBlank
    @NotNull
    private String lastName;

}

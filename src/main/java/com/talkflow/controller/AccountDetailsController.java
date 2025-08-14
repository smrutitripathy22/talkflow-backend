package com.talkflow.controller;

import com.talkflow.dto.ResponseDTO;
import com.talkflow.dto.auth.PasswordChangeRequest;
import com.talkflow.dto.auth.ProfileUpdateDTO;
import com.talkflow.dto.auth.UserData;
import com.talkflow.entity.auth.User;
import com.talkflow.service.AccountDetailsService;
import com.talkflow.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountDetailsController {
    private final AccountDetailsService accountDetailsService;

    @GetMapping("/details")
    public ResponseEntity<ResponseDTO<UserData>> accountDetails(@AuthenticationPrincipal User user) {
        UserData userData = accountDetailsService.accountDetails(user.getUserId());
        return ResponseEntity.ok(ResponseBuilder.success(userData, "User details fetched successfully"));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<ResponseDTO<Object>> accountDeactivate(@AuthenticationPrincipal User user) {
        accountDetailsService.accountDeactivation(user.getUserId());
        return ResponseEntity.ok(ResponseBuilder.success("Account Deactivated Successfully...!!"));
    }

    @PostMapping("/update-password")
    public ResponseEntity<ResponseDTO<Object>> changePassword(@RequestBody PasswordChangeRequest changeRequest, @AuthenticationPrincipal User user) {
        accountDetailsService.passwordUpdate(changeRequest, user);
        return ResponseEntity.ok(ResponseBuilder.success("Password updated successfully...!!"));
    }

    @PostMapping("/update-profile")
    public ResponseEntity<ResponseDTO<Object>> updateProfile(@RequestBody ProfileUpdateDTO profileUpdateDTO, @AuthenticationPrincipal User user) {
        accountDetailsService.profileUpdate(profileUpdateDTO, user);
        return ResponseEntity.ok(ResponseBuilder.success("Profile updated successfully...!!"));
    }
}

package com.talkflow.controller;

import com.talkflow.dto.ResponseDTO;
import com.talkflow.dto.auth.*;
import com.talkflow.service.AuthService;
import com.talkflow.util.ResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")

@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<AuthenticationResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse authResponse = authService.register(request);
        ResponseDTO<AuthenticationResponse> response = ResponseBuilder.success(authResponse, "Registration successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<AuthenticationResponse>> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationResponse authResponse = authService.authenticate(request);
        ResponseDTO<AuthenticationResponse> response = ResponseBuilder.success(authResponse, "Login successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<ResponseDTO<AuthenticationResponse>> verifyUser(@RequestParam String token) {
        AuthenticationResponse authResponse = authService.verifyEmail(token);
        ResponseDTO<AuthenticationResponse> response = ResponseBuilder.success(authResponse, "Email verified successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification-token")
    public ResponseEntity<ResponseDTO<Object>> resendVerificationToken(@RequestParam String email) {
            String message = authService.resendVerificationToken(email);
            ResponseDTO<Object> response = ResponseBuilder.success(message);
            return ResponseEntity.ok(response);

    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseDTO<Object>> requestPasswordReset(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {

            String message = authService.requestPasswordReset(forgotPasswordRequest.getEmail());
            return ResponseEntity.ok(ResponseBuilder.success(message, null));

    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseDTO<Object>> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) {

            String message = authService.resetPassword(passwordResetRequest.getToken(), passwordResetRequest.getPassword());
            return ResponseEntity.ok(ResponseBuilder.success(message, null));

    }
}

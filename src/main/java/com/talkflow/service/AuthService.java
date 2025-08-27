package com.talkflow.service;

import com.talkflow.configuration.jwt.JWTService;
import com.talkflow.dto.auth.AuthenticationRequest;
import com.talkflow.dto.auth.AuthenticationResponse;
import com.talkflow.dto.auth.RegisterRequest;
import com.talkflow.dto.auth.UserData;
import com.talkflow.entity.auth.AuthProvider;
import com.talkflow.entity.auth.User;
import com.talkflow.entity.auth.VerificationToken;
import com.talkflow.exception.CustomEmailExistException;
import com.talkflow.repository.ChatMessageRepo;
import com.talkflow.repository.auth.UserRepository;
import com.talkflow.repository.auth.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ChatMessageRepo chatMessageRepo;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailSender emailSender;

    private String generateOTP(int length) {
        StringBuilder token = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            token.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return token.toString();
    }

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomEmailExistException("This Email ID is already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .isVerified(false)
                .build();

        userRepository.save(user);

        createAndSendVerificationToken(user);

        var jwtToken = jwtService.generateToken(user);
        return buildAuthResponse(user, jwtToken);
    }

    private void createAndSendVerificationToken(User user) {
        String token = generateOTP(8);
        verificationTokenRepository.deleteByUserAndType(user, VerificationToken.TokenType.ACCOUNT_VERIFICATION);
        verificationTokenRepository.save(new VerificationToken(token, user, VerificationToken.TokenType.ACCOUNT_VERIFICATION, 24 * 60));
        String verifyLink = "http://localhost:3000/verify-account";
        emailSender.sendVerificationEmail(user.getEmail(), verifyLink, token);
    }


    public AuthenticationResponse verifyEmail(String token) {
        VerificationToken vToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (vToken.getType() != VerificationToken.TokenType.ACCOUNT_VERIFICATION) {
            throw new RuntimeException("Invalid token type");
        }

        if (vToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = vToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(vToken);

        var jwtToken = jwtService.generateToken(user);
        return buildAuthResponse(user, jwtToken);
    }

    @Transactional
    public String resendVerificationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        if (user.getIsVerified()) {
            return "This account is already verified.";
        }

        Optional<VerificationToken> tokenOptional = verificationTokenRepository.findByUserAndType(user, VerificationToken.TokenType.ACCOUNT_VERIFICATION);


        if (tokenOptional.isPresent() && tokenOptional.get().getExpiryDate().isAfter(LocalDateTime.now())) {
            return "A valid token has been sent recently. Please check your email or try again in a few minutes.";
        }


        createAndSendVerificationToken(user);

        return "A new verification email has been sent.";
    }


    @Transactional
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid Email "));


        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new RuntimeException("Account not verified yet , please verify it first");
        }

        if (AuthProvider.GOOGLE.equals(user.getAuthProvider())) {
            throw new RuntimeException("Password reset not available for Google login accounts.");
        }


        verificationTokenRepository.deleteByUserAndType(user, VerificationToken.TokenType.PASSWORD_RESET);

        String token = generateOTP(8);
        verificationTokenRepository.save(
                new VerificationToken(token, user, VerificationToken.TokenType.PASSWORD_RESET, 20)
        );


        String resetLink = "http://localhost:3000/reset-password/" + token;


        emailSender.sendPasswordResetEmail(user.getEmail(), resetLink);

        return "If this email is registered, a reset link has been sent.";
    }


    @Transactional
    public String resetPassword(String token, String newPassword) {
        VerificationToken vToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (vToken.getType() != VerificationToken.TokenType.PASSWORD_RESET) {
            throw new RuntimeException("Invalid token type");
        }

        if (vToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(vToken);
            throw new RuntimeException("Password reset token expired. Please request a new one.");
        }

        User user = vToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        verificationTokenRepository.delete(vToken);

        return "Password successfully updated.";
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = (User) authentication.getPrincipal();
        var jwtToken = jwtService.generateToken(user);

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            userRepository.save(user);
        }

        return buildAuthResponse(user, jwtToken);
    }


    private AuthenticationResponse buildAuthResponse(User user, String jwtToken) {
        UserData userData = UserData.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .chatsExist(chatMessageRepo.existsChatWithActiveRecipient(user.getUserId()))
                .profileUrl(user.getProfile_url())
                .canChangePassword(user.getAuthProvider().equals(AuthProvider.LOCAL))
                .build();

        return AuthenticationResponse.builder()
                .userData(userData)
                .token(jwtToken)
                .build();
    }

}
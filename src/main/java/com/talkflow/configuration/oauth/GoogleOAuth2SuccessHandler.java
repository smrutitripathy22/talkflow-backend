package com.talkflow.configuration.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkflow.configuration.jwt.JWTService;

import com.talkflow.dto.auth.AuthenticationResponse;
import com.talkflow.dto.auth.UserData;
import com.talkflow.entity.auth.AuthProvider;
import com.talkflow.entity.auth.User;
import com.talkflow.repository.ChatMessageRepo;
import com.talkflow.repository.auth.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final ChatMessageRepo chatMessageRepo;
    private final ObjectMapper objectMapper; // to write JSON

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String firstName = oauthUser.getAttribute("given_name");
        String middleName="";
        String lastName = oauthUser.getAttribute("family_name");
        String picture = oauthUser.getAttribute("picture");


        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName);
                    newUser.setLastName(lastName);
                    newUser.setMiddleName(middleName);
                    newUser.setIsActive(true);
                    newUser.setIsVerified(true);
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    return userRepository.save(newUser);
                });

        // Ensure user is active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            userRepository.save(user);
        }

        // Generate JWT
        String jwtToken = jwtService.generateToken(user);

        // Build UserData (same as in authenticate())
        UserData userData = UserData.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .chatsExist(chatMessageRepo.existsChatWithActiveRecipient(user.getUserId()))
                .profileUrl("")
                .build();

        // Build AuthenticationResponse (same as in authenticate())
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .userData(userData)
                .token(jwtToken)
                .build();

        // Write JSON to response
        String userJson = URLEncoder.encode(objectMapper.writeValueAsString(userData), "UTF-8");
        String redirectUrl = "http://localhost:3000/login?google=success&token=" + jwtToken + "&user=" + userJson;
        response.sendRedirect(redirectUrl);

    }
}

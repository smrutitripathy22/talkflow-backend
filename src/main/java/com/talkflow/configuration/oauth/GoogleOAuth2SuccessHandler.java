package com.talkflow.configuration.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkflow.configuration.jwt.JWTService;

import com.talkflow.dto.auth.AuthenticationResponse;
import com.talkflow.dto.auth.UserData;
import com.talkflow.entity.auth.AuthProvider;
import com.talkflow.entity.auth.User;
import com.talkflow.repository.ChatMessageRepo;
import com.talkflow.repository.auth.UserRepository;

import com.talkflow.service.S3Service;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    @Value("${app.oauth2.redirect-url}")
    private String oauthRedirectUrl;

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
                    String profile = s3Service.uploadGoogleProfile(picture, email);
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName);
                    newUser.setLastName(lastName);
                    newUser.setMiddleName(middleName);
                    newUser.setIsActive(true);
                    newUser.setIsVerified(true);
                    newUser.setProfile_url(profile);
                    newUser.setAuthProvider(AuthProvider.GOOGLE);
                    return userRepository.save(newUser);
                });

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            userRepository.save(user);
        }


        String jwtToken = jwtService.generateToken(user);


        UserData userData = UserData.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .chatsExist(chatMessageRepo.existsChatWithActiveRecipient(user.getUserId()))
                .profileUrl(user.getProfile_url())
                .build();


        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .userData(userData)
                .token(jwtToken)
                .build();


        String userJson = URLEncoder.encode(objectMapper.writeValueAsString(userData), "UTF-8");
        String redirectUrl = oauthRedirectUrl + "?google=success&token=" + jwtToken + "&user=" + userJson;
        response.sendRedirect(redirectUrl);

    }
}

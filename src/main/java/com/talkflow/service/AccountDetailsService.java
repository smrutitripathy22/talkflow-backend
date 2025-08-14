package com.talkflow.service;


import com.talkflow.dto.auth.PasswordChangeRequest;
import com.talkflow.dto.auth.ProfileUpdateDTO;
import com.talkflow.dto.auth.UserData;
import com.talkflow.entity.auth.AuthProvider;
import com.talkflow.entity.auth.User;
import com.talkflow.repository.auth.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserData accountDetails(Long currentUserId) {
        User user = userRepository.findById(currentUserId).orElseThrow(() -> new EntityNotFoundException("User does not exist"));
        return UserData.builder().userId(user.getUserId()).firstName(user.getFirstName()).middleName(user.getMiddleName()).lastName(user.getLastName()).email(user.getEmail()).profileUrl("").canChangePassword(user.getAuthProvider() == AuthProvider.LOCAL).build();


    }

    @Transactional
    public void accountDeactivation(Long currentUserId) {
        User user = userRepository.findById(currentUserId).orElseThrow(() -> new EntityNotFoundException("User does not exist"));
        user.setIsActive(false);
        userRepository.save(user);

    }


    @Transactional
    public void passwordUpdate(PasswordChangeRequest request, User user) {


        if (user.getAuthProvider() != null && user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new IllegalArgumentException("Password change not allowed for Google OAuth users. Please change your password via your Google account.");
        }


        if (request.getPassword().equals(request.getUpdatePassword())) {
            throw new IllegalArgumentException("You cannot have updated password as your same");
        }


        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword()));


        user.setPassword(passwordEncoder.encode(request.getUpdatePassword()));
        userRepository.save(user);
    }

    @Transactional
    public void profileUpdate(ProfileUpdateDTO profileUpdateDTO, User user) {
        user.setFirstName(profileUpdateDTO.getFirstName());
        user.setMiddleName(profileUpdateDTO.getMiddleName());
        user.setLastName(profileUpdateDTO.getLastName());
        userRepository.save(user);
    }
}

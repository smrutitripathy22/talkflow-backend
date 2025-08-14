package com.talkflow.repository.auth;


import com.talkflow.entity.auth.User;
import com.talkflow.entity.auth.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUserAndType(User user, VerificationToken.TokenType type);

    void deleteByUserAndType(User user, VerificationToken.TokenType type);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken v WHERE v.expiryDate < :dateTime")
    int deleteByExpiryDateBefore(LocalDateTime dateTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken v WHERE v.type = :type AND v.expiryDate < :dateTime")
    int deleteByTypeAndExpiryDateBefore(VerificationToken.TokenType type, LocalDateTime dateTime);

}


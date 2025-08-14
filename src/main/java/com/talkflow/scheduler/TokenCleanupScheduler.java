package com.talkflow.scheduler;

import com.talkflow.entity.auth.VerificationToken.TokenType;

import com.talkflow.repository.auth.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final VerificationTokenRepository verificationTokenRepository;


    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void removeExpiredPasswordResetTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = verificationTokenRepository.deleteByTypeAndExpiryDateBefore(TokenType.PASSWORD_RESET, now);

        log.info("Deleted {} expired PASSWORD_RESET tokens at {}", deletedCount, now);
    }
}

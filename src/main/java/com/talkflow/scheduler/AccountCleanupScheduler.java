package com.talkflow.scheduler;

import com.talkflow.entity.auth.VerificationToken.TokenType;

import com.talkflow.repository.auth.UserRepository;
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
public class AccountCleanupScheduler {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;


    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void removeOldUnverifiedAccounts() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        int deletedUsers = userRepository.deleteUnverifiedOlderThan(cutoffDate);


        int deletedTokens = verificationTokenRepository.deleteByTypeAndExpiryDateBefore(TokenType.ACCOUNT_VERIFICATION, cutoffDate);

        log.info("Deleted {} unverified accounts and {} ACCOUNT_VERIFICATION tokens older than {}", deletedUsers, deletedTokens, cutoffDate);
    }
}

package com.talkflow.entity.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_user_id", "type"})
)
public class VerificationToken {

    public enum TokenType {
        ACCOUNT_VERIFICATION,
        PASSWORD_RESET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType type;

    private LocalDateTime expiryDate;

    public VerificationToken() {}

    public VerificationToken(String token, User user, TokenType type, long expiryMinutes) {
        this.token = token;
        this.user = user;
        this.type = type;
        this.expiryDate = LocalDateTime.now().plusMinutes(expiryMinutes);
    }
}

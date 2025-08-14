package com.talkflow.entity.chatMessage;

import com.talkflow.entity.auth.User;
import com.talkflow.entity.group.Group;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chatId;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;


    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = true)
    private User recipient;


    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private String content;

    @CreationTimestamp
    private LocalDateTime timestamp;
}

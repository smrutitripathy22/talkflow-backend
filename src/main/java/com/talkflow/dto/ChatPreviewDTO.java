package com.talkflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatPreviewDTO {
    private Long userId;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private boolean canSendMsg;
}

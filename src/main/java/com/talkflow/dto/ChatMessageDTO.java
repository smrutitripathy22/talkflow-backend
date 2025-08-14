package com.talkflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageDTO {
    private String to;
    private String from;
    private String content;
    private LocalDateTime timestamp;
}
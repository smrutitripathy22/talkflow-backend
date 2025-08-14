package com.talkflow.dto.group;

import com.talkflow.dto.auth.UserData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GroupChats {
    private Long groupId;
    private String chatId;
    private String content;
    private LocalDateTime timestamp;
    private String from;
    private UserData user;
}

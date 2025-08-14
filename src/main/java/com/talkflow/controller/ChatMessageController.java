package com.talkflow.controller;

import com.talkflow.dto.ChatMessageDTO;
import com.talkflow.dto.ChatPreviewDTO;
import com.talkflow.dto.ResponseDTO;
import com.talkflow.entity.auth.User;
import com.talkflow.service.ChatMessageService;
import com.talkflow.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("chat")
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @GetMapping("{recipientId}")
    public ResponseEntity<ResponseDTO<List<ChatMessageDTO>>> getChatMessages(@AuthenticationPrincipal User sender, @PathVariable Long recipientId) {
        List<ChatMessageDTO> messages = chatMessageService.getChatMessages(sender.getUserId(), recipientId);
        ResponseDTO<List<ChatMessageDTO>> response = ResponseBuilder.success(messages, "Chat messages fetched successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("previews")
    public ResponseEntity<ResponseDTO<List<ChatPreviewDTO>>> getChatPreviews(@AuthenticationPrincipal User sender) {
        List<ChatPreviewDTO> previews = chatMessageService.getChatPreviews(sender.getUserId());
        ResponseDTO<List<ChatPreviewDTO>> response = ResponseBuilder.success(previews, "Chat previews fetched successfully");
        return ResponseEntity.ok(response);
    }

}

package com.talkflow.service;

import com.talkflow.dto.ChatMessageDTO;
import com.talkflow.dto.ChatPreviewDTO;
import com.talkflow.entity.auth.User;
import com.talkflow.entity.chatMessage.ChatMessage;
import com.talkflow.entity.connection.UserConnection;
import com.talkflow.repository.ChatMessageRepo;
import com.talkflow.repository.UserConnectionRepo;
import com.talkflow.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class ChatMessageService {
    private final ChatMessageRepo chatMessageRepo;
    private final UserConnectionRepo userConnectionRepo;
    private final UserRepository userRepository;

    public List<ChatPreviewDTO> getChatPreviews(Long userId) {
        User currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Current user not found"));
        if (!Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new RuntimeException("Your account is deactivated");
        }

        List<UserConnection> connections = userConnectionRepo.getAllAcceptedRequest(userId);


        Set<Long> otherUserIds = connections.stream().map(conn -> conn.getSenderId().equals(userId) ? conn.getReceiverId() : conn.getSenderId()).collect(Collectors.toSet());


        Map<Long, User> userMap = userRepository.getAllUsersById(otherUserIds).stream().collect(Collectors.toMap(User::getUserId, Function.identity()));


        List<ChatMessage> allMessages = chatMessageRepo.findChatsByUserId(userId);


        Map<String, ChatMessage> latestMessages = allMessages.stream().collect(Collectors.toMap(msg -> {
            Long id1 = msg.getSender().getUserId();
            Long id2 = msg.getRecipient().getUserId();
            return id1 < id2 ? id1 + "_" + id2 : id2 + "_" + id1;
        }, msg -> msg, (m1, m2) -> m1.getTimestamp().isAfter(m2.getTimestamp()) ? m1 : m2));


        List<ChatPreviewDTO> previews = connections.stream().map(conn -> {

            Long otherId = conn.getSenderId().equals(userId) ? conn.getReceiverId() : conn.getSenderId();
            User otherUser = userMap.get(otherId);
            if (otherUser == null) return null;
            String chatKey = userId < otherId ? userId + "_" + otherId : otherId + "_" + userId;
            ChatMessage lastMsg = latestMessages.get(chatKey);


            return ChatPreviewDTO.builder().canSendMsg(otherUser.getIsActive()).userId(otherUser.getUserId()).firstName(otherUser.getFirstName()).middleName(otherUser.getMiddleName()).lastName(otherUser.getLastName()).email(otherUser.getEmail()).lastMessage(lastMsg != null ? lastMsg.getContent() : null).lastMessageTime(lastMsg != null ? lastMsg.getTimestamp() : null).build();
        }).filter(Objects::nonNull).sorted(Comparator.comparing(ChatPreviewDTO::getLastMessageTime, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());

        return previews;
    }


    public List<ChatMessageDTO> getChatMessages(Long senderId, Long recipientId) {
        var chatId = createChatId(senderId, recipientId);
        List<ChatMessage> chatMessages = chatMessageRepo.findByChatId(chatId);
        List<ChatMessageDTO> chatMessageDTOList = new ArrayList<>();
        for (ChatMessage cm : chatMessages) {
            chatMessageDTOList.add(ChatMessageDTO.builder().from(cm.getSender().getEmail()).to(cm.getRecipient().getEmail()).content(cm.getContent()).timestamp(cm.getTimestamp()).build());

        }
        return chatMessageDTOList;
    }


    private String createChatId(Long senderId, Long recipientId) {
        if (senderId > recipientId) {
            return recipientId + "_" + senderId;
        } else {
            return senderId + "_" + recipientId;
        }
    }
}

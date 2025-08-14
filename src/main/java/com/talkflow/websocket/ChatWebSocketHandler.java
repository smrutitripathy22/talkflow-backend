package com.talkflow.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkflow.entity.chatMessage.ChatMessage;
import com.talkflow.entity.chatMessage.MessageType;
import com.talkflow.repository.ChatMessageRepo;
import com.talkflow.repository.UserConnectionRepo;
import com.talkflow.repository.auth.UserRepository;
import com.talkflow.repository.group.GroupMemberRepo;
import com.talkflow.repository.group.GroupRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatMessageRepo chatMessageRepo;
    private final UserRepository userRepository;
    private final UserConnectionRepo userConnectionRepo;
    private final GroupMemberRepo groupMemberRepo;
    private final GroupRepo groupRepo;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");

        if (username != null) {
            sessions.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(session);
            logger.info("User connected: {} (sessions={})", username, sessions.get(username).size());
        } else {
            logger.error("No username found in attributes — closing connection");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("User not authenticated"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode jsonMessage = objectMapper.readTree(message.getPayload());
        String messageType = jsonMessage.has("type") ? jsonMessage.get("type").asText() : "private";

        switch (messageType) {
            case "group":
                handleGroupMessage(session, jsonMessage);
                break;
            case "call-request":
            case "call-accept":
            case "call-decline":
            case "call-end":
                handleCallSignal(session, jsonMessage);
                break;
            case "signal":
                handleWebRTCSignal(session, jsonMessage);
                break;
            default:
                handlePrivateMessage(session, jsonMessage);
                break;
        }
    }

    private void handlePrivateMessage(WebSocketSession session, JsonNode jsonMessage) {
        String senderEmail = (String) session.getAttributes().get("username");
        if (senderEmail == null) return;

        String recipientEmail = jsonMessage.get("to").asText();
        String content = jsonMessage.get("content").asText();

        var sender = userRepository.findByEmail(senderEmail).orElseThrow();
        var recipient = userRepository.findByEmail(recipientEmail).orElseThrow(() -> new RuntimeException("Recipient not found"));

        if (userConnectionRepo.findAcceptedConnectionBetween(sender.getUserId(), recipient.getUserId()).isEmpty()) {
            logger.error("Private messaging not allowed without accepted connection.");
            return;
        }

        var chatMessage = ChatMessage.builder().chatId(createChatId(sender.getUserId(), recipient.getUserId())).sender(sender).recipient(recipient).content(content).type(MessageType.TEXT).build();
        chatMessageRepo.save(chatMessage);

        sendToUser(senderEmail, jsonMessage);
        sendToUser(recipientEmail, jsonMessage);
    }

    private void handleGroupMessage(WebSocketSession session, JsonNode jsonMessage) {
        String senderEmail = (String) session.getAttributes().get("username");
        if (senderEmail == null) {
            logger.error("Group message sender not authenticated");
            return;
        }

        long groupId = jsonMessage.get("groupId").asLong();
        String content = jsonMessage.get("content").asText();

        var sender = userRepository.findByEmail(senderEmail).orElseThrow();
        var group = groupRepo.findById(groupId).orElseThrow();

        var chatMessage = ChatMessage.builder().chatId("group_" + groupId).sender(sender).group(group).content(content).type(MessageType.TEXT).build();
        chatMessageRepo.save(chatMessage);

        groupMemberRepo.findAllByGroupId(groupId).forEach(member -> sendToUser(member.getUser().getEmail(), jsonMessage));
    }

    private void handleWebRTCSignal(WebSocketSession session, JsonNode jsonMessage) {
        String recipientEmail = jsonMessage.has("to") ? jsonMessage.get("to").asText() : null;
        if (recipientEmail != null) {
            sendToUser(recipientEmail, jsonMessage);
        }
    }

    private void handleCallSignal(WebSocketSession session, JsonNode jsonMessage) {
        String recipientEmail = jsonMessage.get("to").asText();
        String type = jsonMessage.get("type").asText();
        String callerEmail = (String) session.getAttributes().get("username");

        if ("call-request".equals(type)) {
            if (!sessions.containsKey(recipientEmail) || sessions.get(recipientEmail).isEmpty()) {

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // If still offline after 20 seconds, notify caller
                        if (!sessions.containsKey(recipientEmail) || sessions.get(recipientEmail).isEmpty()) {
                            Map<String, String> endPayload = new HashMap<>();
                            endPayload.put("type", "call-end");
                            endPayload.put("to", callerEmail);
                            endPayload.put("reason", "Recipient not available");

                            sendToUser(callerEmail, endPayload);
                            logger.info("Call ended automatically — recipient still offline after 20s");
                        }
                    }
                }, 20_000);
            }
        }

        sendToUser(recipientEmail, jsonMessage);
    }

    private void sendToUser(String email, Object payload) {
        List<WebSocketSession> userSessions = sessions.get(email);
        if (userSessions != null) {
            for (WebSocketSession ws : userSessions) {
                if (ws.isOpen()) {
                    try {
                        ws.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                    } catch (IOException e) {
                        logger.error("Error sending message to {}: {}", email, e.getMessage());
                    }
                }
            }
        } else {
            logger.info("User offline: {}", email);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UserDetails userDetails = extractUserDetails(session);
        if (userDetails != null) {
            String username = userDetails.getUsername();
            List<WebSocketSession> userSessions = sessions.get(username);

            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) {
                    sessions.remove(username);
                }
            }
            logger.info("User disconnected: {} (remainingSessions={})", username, sessions.getOrDefault(username, List.of()).size());
        }
    }

    private UserDetails extractUserDetails(WebSocketSession session) {
        Object userAttr = session.getAttributes().get("user");
        if (userAttr instanceof UserDetails) return (UserDetails) userAttr;
        if (session.getPrincipal() instanceof UserDetails) return (UserDetails) session.getPrincipal();
        return null;
    }

    private String createChatId(Long senderId, Long recipientId) {
        return senderId < recipientId ? senderId + "_" + recipientId : recipientId + "_" + senderId;
    }
}

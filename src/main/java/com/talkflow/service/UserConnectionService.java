package com.talkflow.service;

import com.talkflow.dto.auth.UserData;
import com.talkflow.dto.userConnections.UserConnectionDTO;
import com.talkflow.entity.auth.User;
import com.talkflow.entity.connection.ConnectionStatus;
import com.talkflow.entity.connection.UserConnection;
import com.talkflow.repository.UserConnectionRepo;
import com.talkflow.repository.auth.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserConnectionService {
    private static final Logger logger = LoggerFactory.getLogger(UserConnectionService.class);
    private final UserConnectionRepo userConnectionRepo;
    private final UserRepository userRepository;

    @Transactional
    public void addConnectionRequest(Long senderId, Long receiverId) {
        logger.info("Received request to connect senderId={} with receiverId={}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            logger.warn("User {} attempted to send a connection request to themselves", senderId);
            throw new IllegalArgumentException("You cannot send request to yourself");
        }

        User sender = userRepository.findById(senderId).orElseThrow(() -> {
            logger.error("Sender with ID {} not found", senderId);
            return new EntityNotFoundException("No user exists with userID " + senderId);
        });

        User receiver = userRepository.findById(receiverId).orElseThrow(() -> {
            logger.error("Receiver with ID {} not found", receiverId);
            return new EntityNotFoundException("No user exists with userID " + receiverId);
        });

        Optional<UserConnection> userConnection = userConnectionRepo.areUserConnected(senderId, receiverId);

        if (userConnection.isPresent()) {
            UserConnection existingConnection = userConnection.get();
            ConnectionStatus status = existingConnection.getStatus();

            logger.info("Existing connection found between {} and {} with status {}", senderId, receiverId, status);

            if (status == ConnectionStatus.UNBLOCKED || status == ConnectionStatus.REJECTED || status == ConnectionStatus.WITHDRAWN) {
                existingConnection.setSenderId(senderId);
                existingConnection.setReceiverId(receiverId);
                existingConnection.setStatus(ConnectionStatus.PENDING);
                userConnectionRepo.save(existingConnection);
                logger.info("Updated connection between {} and {} to PENDING", senderId, receiverId);
            } else {
                logger.warn("Connection request blocked due to existing status: {}", status);
                throw new IllegalArgumentException("Request sending failed: already existing connection");
            }
        } else {
            UserConnection newConnection = UserConnection.builder().senderId(senderId).receiverId(receiverId).status(ConnectionStatus.PENDING).build();
            userConnectionRepo.save(newConnection);
            logger.info("New connection request created from {} to {}", senderId, receiverId);
        }
    }

    @Transactional
    public void addBlockRequest(Long senderId, Long receiverId) {
        logger.info("Received block request from senderId={} to block receiverId={}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            logger.warn("User {} attempted to block themselves", senderId);
            throw new IllegalArgumentException("You cannot block yourself");
        }

        User sender = userRepository.findById(senderId).orElseThrow(() -> {
            logger.error("Sender with ID {} not found", senderId);
            return new EntityNotFoundException("No user exists with userID " + senderId);
        });

        User receiver = userRepository.findById(receiverId).orElseThrow(() -> {
            logger.error("Receiver with ID {} not found", receiverId);
            return new EntityNotFoundException("No user exists with userID " + receiverId);
        });

        Optional<UserConnection> userConnection = userConnectionRepo.areUserConnected(senderId, receiverId);

        if (userConnection.isPresent()) {
            UserConnection existingConnection = userConnection.get();
            ConnectionStatus status = existingConnection.getStatus();

            logger.info("Existing connection found between {} and {} with status {}", senderId, receiverId, status);

            if (status != ConnectionStatus.BLOCKED) {
                existingConnection.setStatus(ConnectionStatus.BLOCKED);
                existingConnection.setBlockedByUserId(senderId);
                userConnectionRepo.save(existingConnection);
                logger.info("Updated connection between {} and {} to BLOCKED", senderId, receiverId);
            } else {
                logger.warn("Block request failed: {} already blocked {}", senderId, receiverId);
                throw new IllegalArgumentException("Block request failed: user is already blocked");
            }
        } else {
            UserConnection newConnection = UserConnection.builder().senderId(senderId).receiverId(receiverId).blockedByUserId(senderId).status(ConnectionStatus.BLOCKED).build();
            userConnectionRepo.save(newConnection);
            logger.info("Created new BLOCKED connection from {} to {}", senderId, receiverId);
        }
    }


    @Transactional
    public void updateStatus(Long currentUserId, Long connectionId, String statusStr) {
        logger.info("Received status update request. userId={}, connectionId={}, newStatus={}", currentUserId, connectionId, statusStr);


        ConnectionStatus newStatus;
        try {
            newStatus = ConnectionStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value: {}", statusStr);
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }

        UserConnection connection = userConnectionRepo.findById(connectionId).orElseThrow(() -> {
            logger.error("Connection not found with id {}", connectionId);
            return new EntityNotFoundException("Connection not found with ID: " + connectionId);
        });

        Long senderId = connection.getSenderId();
        Long receiverId = connection.getReceiverId();


        if (!currentUserId.equals(senderId) && !currentUserId.equals(receiverId)) {
            logger.warn("User {} is not authorized to update connection {}", currentUserId, connectionId);
            throw new SecurityException("You are not a participant in this connection.");
        }

        ConnectionStatus currentStatus = connection.getStatus();


        if (newStatus == ConnectionStatus.PENDING) {
            logger.warn("Attempted invalid transition to PENDING for connection {}", connectionId);
            throw new IllegalArgumentException("Cannot change status back to PENDING.");
        }

        if (newStatus == ConnectionStatus.ACCEPTED || newStatus == ConnectionStatus.REJECTED) {
            if (!currentUserId.equals(receiverId)) {
                logger.warn("User {} is not receiver and cannot {} request", currentUserId, newStatus);
                throw new SecurityException("Only the receiver can " + newStatus.name().toLowerCase() + " the request.");
            }

            if (currentStatus != ConnectionStatus.PENDING) {
                logger.warn("Request is not in PENDING state. Cannot change to {}", newStatus);
                throw new IllegalStateException("Can only " + newStatus.name().toLowerCase() + " a pending request.");
            }

            connection.setStatus(newStatus);
            userConnectionRepo.save(connection);
            logger.info("Connection {} updated to {}", connectionId, newStatus);
            return;
        }

        if (newStatus == ConnectionStatus.WITHDRAWN) {
            if (currentStatus != ConnectionStatus.PENDING) {
                logger.warn("Cannot withdraw a connection that's not PENDING. ConnectionId={}", connectionId);
                throw new IllegalStateException("Only pending requests can be withdrawn.");
            }

            if (!currentUserId.equals(connection.getSenderId())) {
                logger.warn("User {} is not the sender and cannot withdraw connection {}", currentUserId, connectionId);
                throw new SecurityException("Only the sender can withdraw the connection request.");
            }

            connection.setStatus(ConnectionStatus.WITHDRAWN);
            userConnectionRepo.save(connection);
            logger.info("Connection {} successfully withdrawn by user {}", connectionId, currentUserId);
            return;
        }

        if (newStatus == ConnectionStatus.UNBLOCKED) {
            if (currentStatus != ConnectionStatus.BLOCKED) {
                logger.warn("Cannot unblock a connection that's not BLOCKED. ConnectionId={}", connectionId);
                throw new IllegalStateException("Connection is not currently blocked.");
            }

            if (!currentUserId.equals(connection.getBlockedByUserId())) {
                logger.warn("User {} is not the blocker and cannot unblock connection {}", currentUserId, connectionId);
                throw new SecurityException("Only the user who blocked the connection can unblock it.");
            }

            connection.setStatus(ConnectionStatus.UNBLOCKED);
            connection.setBlockedByUserId(null);
            userConnectionRepo.save(connection);
            logger.info("Connection {} successfully unblocked by user {}", connectionId, currentUserId);
            return;
        }


        logger.warn("Unsupported status update attempted: {}", newStatus);
        throw new IllegalArgumentException("Unsupported status transition to: " + newStatus);
    }


    public List<UserConnectionDTO> getAllSentRequest(Long currentUserId) {
        List<UserConnection> userConnections = userConnectionRepo.getAllSentRequest(currentUserId);

        Set<Long> receiverIds = userConnections.stream().map(UserConnection::getReceiverId).collect(Collectors.toSet());


        List<User> users = userRepository.findAllById(receiverIds);


        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUserId, Function.identity()));


        List<UserConnectionDTO> userConnectionDTOList = new ArrayList<>();
        for (UserConnection uc : userConnections) {
            User user = userMap.get(uc.getReceiverId());
            if (user == null) {
                throw new EntityNotFoundException("User not found with ID: " + uc.getReceiverId());
            }

            UserConnectionDTO userConnectionDTO = UserConnectionDTO.builder().connectionId(uc.getId()).userId(uc.getReceiverId()).firstName(user.getFirstName()).middleName(user.getMiddleName()).lastName(user.getLastName()).email(user.getEmail()).status(uc.getStatus().toString()).build();

            userConnectionDTOList.add(userConnectionDTO);
        }

        return userConnectionDTOList;
    }

    public List<UserConnectionDTO> getAllPendingRequest(Long currentUserId) {
        List<UserConnection> userConnections = userConnectionRepo.getAllPendingRequest(currentUserId);

        Set<Long> senderIds = userConnections.stream().map(UserConnection::getSenderId).collect(Collectors.toSet());


        List<User> users = userRepository.findAllById(senderIds);


        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUserId, Function.identity()));


        List<UserConnectionDTO> userConnectionDTOList = new ArrayList<>();
        for (UserConnection uc : userConnections) {
            User user = userMap.get(uc.getSenderId());
            if (user == null) {
                throw new EntityNotFoundException("User not found with ID: " + uc.getSenderId());
            }

            UserConnectionDTO userConnectionDTO = UserConnectionDTO.builder().connectionId(uc.getId()).userId(uc.getSenderId()).firstName(user.getFirstName()).middleName(user.getMiddleName()).lastName(user.getLastName()).email(user.getEmail()).status(uc.getStatus().toString()).build();

            userConnectionDTOList.add(userConnectionDTO);
        }

        return userConnectionDTOList;
    }

    public List<UserConnectionDTO> getAllBlockedRequest(Long currentUserId) {
        List<UserConnection> userConnections = userConnectionRepo.getAllBlockedRequest(currentUserId);

        Set<Long> blockedUserIds = userConnections.stream().map(uc -> uc.getSenderId().equals(uc.getBlockedByUserId()) ? uc.getReceiverId() : uc.getSenderId()).collect(Collectors.toSet());


        List<User> users = userRepository.findAllById(blockedUserIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<UserConnectionDTO> userConnectionDTOList = new ArrayList<>();

        for (UserConnection uc : userConnections) {
            Long blockedUserId = uc.getSenderId().equals(uc.getBlockedByUserId()) ? uc.getReceiverId() : uc.getSenderId();

            User user = userMap.get(blockedUserId);
            if (user == null) {
                throw new EntityNotFoundException("Blocked user not found with ID: " + blockedUserId);
            }

            UserConnectionDTO dto = UserConnectionDTO.builder().connectionId(uc.getId()).userId(blockedUserId).firstName(user.getFirstName()).middleName(user.getMiddleName()).lastName(user.getLastName()).email(user.getEmail()).status(uc.getStatus().toString()).build();

            userConnectionDTOList.add(dto);
        }

        return userConnectionDTOList;
    }

    public List<UserConnectionDTO> getAllAcceptedRequest(Long currentUserId) {

        List<UserConnection> userConnections = userConnectionRepo.getAllAcceptedRequest(currentUserId);


        Set<Long> connectedUserIds = userConnections.stream().map(uc -> uc.getSenderId().equals(currentUserId) ? uc.getReceiverId() : uc.getSenderId()).collect(Collectors.toSet());


        List<User> users = userRepository.findAllById(connectedUserIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUserId, Function.identity()));


        List<UserConnectionDTO> userConnectionDTOList = new ArrayList<>();
        for (UserConnection uc : userConnections) {
            Long otherUserId = uc.getSenderId().equals(currentUserId) ? uc.getReceiverId() : uc.getSenderId();
            User user = userMap.get(otherUserId);
            if (user == null) {
                throw new EntityNotFoundException("Connected user not found with ID: " + otherUserId);
            }

            UserConnectionDTO dto = UserConnectionDTO.builder().connectionId(uc.getId()).userId(otherUserId).firstName(user.getFirstName()).middleName(user.getMiddleName()).lastName(user.getLastName()).email(user.getEmail()).status(uc.getStatus().toString()).build();

            userConnectionDTOList.add(dto);
        }

        return userConnectionDTOList;
    }

    public List<UserData> getAllUnconnectedUser(Long currentUserId) {

        List<User> allUsers = userRepository.getAllRecords();

        List<UserConnection> existingConnections = userConnectionRepo.getAllRequestWithSomeConnection(currentUserId, List.of(ConnectionStatus.ACCEPTED, ConnectionStatus.BLOCKED, ConnectionStatus.PENDING));

        Set<Long> connectedUserIds = existingConnections.stream().map(uc -> uc.getSenderId().equals(currentUserId) ? uc.getReceiverId() : uc.getSenderId()).collect(Collectors.toSet());

        return allUsers.stream().filter(user -> !user.getUserId().equals(currentUserId))
                .filter(user -> !connectedUserIds.contains(user.getUserId()))
                .map(user -> UserData.builder().userId(user.getUserId()).firstName(user.getFirstName()).middleName(user.getMiddleName()).lastName(user.getLastName()).email(user.getEmail()).profileUrl("").build()).collect(Collectors.toList());
    }


}

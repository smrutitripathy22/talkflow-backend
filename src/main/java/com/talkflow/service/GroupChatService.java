package com.talkflow.service;


import com.talkflow.dto.auth.UserData;
import com.talkflow.dto.group.GroupChats;
import com.talkflow.dto.group.GroupRequest;
import com.talkflow.entity.auth.User;
import com.talkflow.entity.chatMessage.ChatMessage;
import com.talkflow.entity.connection.UserConnection;
import com.talkflow.entity.group.Group;
import com.talkflow.entity.group.GroupMember;
import com.talkflow.repository.ChatMessageRepo;
import com.talkflow.repository.UserConnectionRepo;
import com.talkflow.repository.auth.UserRepository;
import com.talkflow.repository.group.GroupMemberRepo;
import com.talkflow.repository.group.GroupRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {
    private final GroupRepo groupRepo;
    private final GroupMemberRepo groupMemberRepo;
    private final UserRepository userRepository;
    private final UserConnectionRepo userConnectionRepo;
    private final ChatMessageRepo chatMessageRepo;

    @Transactional
    public Group createGroup(GroupRequest request, User creator) {


        User creatorUser = userRepository.findById(creator.getUserId()).orElseThrow(() -> new RuntimeException("Creator not found"));

        if (!Boolean.TRUE.equals(creatorUser.getIsActive())) {
            throw new RuntimeException("Your account is deactivated");
        }

        Group group = Group.builder().groupName(request.getGroupName()).createdBy(creatorUser).build();
        groupRepo.save(group);

        GroupMember member = GroupMember.builder().group(group).user(creatorUser).build();

        groupMemberRepo.save(member);

        return group;
    }

    @Transactional
    public void addMember(Long groupId, Long userId, Long currentUserId) {
        Group group = groupRepo.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));

        User userToAdd = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User to add not found"));

        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Current user not found"));

        if (!Boolean.TRUE.equals(userToAdd.getIsActive())) {
            throw new RuntimeException("User to add has no active account");
        }

        if (!Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new RuntimeException("Your account is deactivated");
        }

        boolean isCurrentUserAdmin = group.getCreatedBy().getUserId().equals(currentUserId);
        if (!isCurrentUserAdmin) {
            throw new RuntimeException("You must be the admin of the group to add others.");
        }

        boolean isConnected = userConnectionRepo.findAcceptedConnectionBetween(currentUserId, userToAdd.getUserId()).isPresent();

        if (!isConnected) {
            throw new RuntimeException("Cannot add user without accepted connection.");
        }

        boolean isAlreadyMember = groupMemberRepo.existsByGroupIdAndUserId(groupId, userToAdd.getUserId());

        if (isAlreadyMember) {
            throw new RuntimeException("User is already a member of the group.");
        }

        GroupMember member = GroupMember.builder().group(group).user(userToAdd).build();

        groupMemberRepo.save(member);
    }


    public List<Group> allGroups(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Current user not found"));
        if (!Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new RuntimeException("Your account is deactivated");
        }
        List<Group> groupList = groupMemberRepo.findAllGroupByUserId(currentUserId);
        return groupList;
    }

    public List<UserData> allMembers(Long currentUserId, Long groupId) {
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Current user not found"));
        if (!Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new RuntimeException("Your account is deactivated");
        }
        boolean isMember = groupMemberRepo.existsByGroupIdAndUserId(groupId, currentUserId);
        if (!isMember) {
            throw new AccessDeniedException("User is not a member of this group.");
        }

        List<User> users = groupMemberRepo.getAllUsersInGroup(groupId);
        List<UserData> userDataList = new ArrayList<>();
        for (User u : users) {
            userDataList.add(UserData.builder().userId(u.getUserId()).email(u.getEmail()).firstName(u.getFirstName()).middleName(u.getMiddleName()).lastName(u.getLastName()).profileUrl(u.getProfile_url()).build());
        }
        return userDataList;

    }

    public List<UserData> allNonMembers(Long currentUserId, Long groupId) {
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Current user not found"));

        if (!Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new RuntimeException("Your account is deactivated");
        }

        boolean isMember = groupMemberRepo.existsByGroupIdAndUserId(groupId, currentUserId);
        if (!isMember) {
            throw new AccessDeniedException("User is not a member of this group.");
        }


        List<User> usersInGroup = groupMemberRepo.getAllUsersInGroup(groupId);
        Set<Long> usersInGroupIds = usersInGroup.stream().map(User::getUserId).collect(Collectors.toSet());


        List<UserConnection> connections = userConnectionRepo.getAllAcceptedRequest(currentUserId);


        Set<Long> connectedUserIds = connections.stream().map(conn -> conn.getSenderId().equals(currentUserId) ? conn.getReceiverId() : conn.getSenderId()).collect(Collectors.toSet());


        connectedUserIds.removeAll(usersInGroupIds);

        List<User> connectedUsersNotInGroup = userRepository.findAllById(connectedUserIds);


        List<UserData> userDataList = new ArrayList<>();
        for (User u : connectedUsersNotInGroup) {
            userDataList.add(UserData.builder().userId(u.getUserId()).email(u.getEmail()).firstName(u.getFirstName()).middleName(u.getMiddleName()).lastName(u.getLastName()).profileUrl(u.getProfile_url()).build());
        }

        return userDataList;
    }


    public List<GroupChats> getAllChatsByGroupId(Long currentUserId, Long groupId) {
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Current user not found"));
        if (!Boolean.TRUE.equals(currentUser.getIsActive())) {
            throw new RuntimeException("Your account is deactivated");
        }
        boolean isMember = groupMemberRepo.existsByGroupIdAndUserId(groupId, currentUserId);
        if (!isMember) {
            throw new AccessDeniedException("User is not a member of this group.");
        }
        List<ChatMessage> chatMessages = chatMessageRepo.findChatsByGroupId(groupId);
        List<GroupChats> groupChats = new ArrayList<>();
        for (ChatMessage cm : chatMessages) {
            User sender = cm.getSender();

            groupChats.add(GroupChats.builder().groupId(cm.getGroup().getId()).chatId(cm.getChatId()).timestamp(cm.getTimestamp()).content(cm.getContent()).from(sender.getEmail()).user(UserData.builder().firstName(sender.getFirstName()).middleName(sender.getMiddleName()).lastName(sender.getLastName()).email(sender.getEmail()).profileUrl(sender.getProfile_url()).build()).build());

        }
        return groupChats;

    }

}

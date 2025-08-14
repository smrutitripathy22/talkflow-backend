package com.talkflow.controller;

import com.talkflow.dto.ResponseDTO;
import com.talkflow.dto.auth.UserData;
import com.talkflow.dto.group.GroupChats;
import com.talkflow.dto.group.GroupRequest;
import com.talkflow.entity.auth.User;
import com.talkflow.entity.group.Group;
import com.talkflow.service.GroupChatService;
import com.talkflow.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

@RequiredArgsConstructor
@RequestMapping("group")
public class GroupChatController {
    private final GroupChatService groupChatService;


    @PostMapping
    public ResponseEntity<ResponseDTO<Group>> createGroup(@RequestBody GroupRequest request, @AuthenticationPrincipal User currentUser) {
        Group createdGroup = groupChatService.createGroup(request, currentUser);
        return ResponseEntity.ok(ResponseBuilder.success(createdGroup, "Group created successfully"));
    }

    @PostMapping("/member")
    public ResponseEntity<ResponseDTO<Object>> addMember(@RequestParam Long groupId, @RequestParam Long memberId, @AuthenticationPrincipal User currentUser) {
        groupChatService.addMember(groupId, memberId, currentUser.getUserId());
        return ResponseEntity.ok(ResponseBuilder.success("Member added successfully"));
    }

    @GetMapping("")
    public ResponseEntity<ResponseDTO<List<Group>>> myGroups(@AuthenticationPrincipal User currentUser) {
        List<Group> groupList = groupChatService.allGroups(currentUser.getUserId());
        return ResponseEntity.ok(ResponseBuilder.success(groupList, "Groups fetched successfully"));
    }

    @GetMapping("/chats/{groupId}")
    public ResponseEntity<ResponseDTO<List<GroupChats>>> groupChats(@AuthenticationPrincipal User currentUser, @PathVariable Long groupId) {
        List<GroupChats> groupChats = groupChatService.getAllChatsByGroupId(currentUser.getUserId(), groupId);
        return ResponseEntity.ok(ResponseBuilder.success(groupChats, "Group chats fetched successfully"));
    }

    @GetMapping("/members/{groupId}")
    public ResponseEntity<ResponseDTO<List<UserData>>> groupMembers(@AuthenticationPrincipal User currentUser, @PathVariable Long groupId) {
        List<UserData> userDataList = groupChatService.allMembers(currentUser.getUserId(), groupId);
        return ResponseEntity.ok(ResponseBuilder.success(userDataList, "Group members fetched successfully"));
    }
    @GetMapping("/non-members/{groupId}")
    public ResponseEntity<ResponseDTO<List<UserData>>> nonGroupMembers(@AuthenticationPrincipal User currentUser, @PathVariable Long groupId) {
        List<UserData> userDataList = groupChatService.allNonMembers(currentUser.getUserId(), groupId);
        return ResponseEntity.ok(ResponseBuilder.success(userDataList, "Group members fetched successfully"));
    }


}

package com.talkflow.controller;

import com.talkflow.dto.ResponseDTO;
import com.talkflow.dto.auth.UserData;
import com.talkflow.dto.userConnections.AllConnectionDTO;
import com.talkflow.dto.userConnections.UserConnectionDTO;
import com.talkflow.entity.auth.User;
import com.talkflow.service.UserConnectionService;
import com.talkflow.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("connection")
@RequiredArgsConstructor
public class UserConnectionController {

    private final UserConnectionService userConnectionService;


    @GetMapping("unconnected")
    public ResponseEntity<ResponseDTO<List<UserData>>> getAllNonConnectedUsers(@AuthenticationPrincipal User currentUser) {
        List<UserData> users = userConnectionService.getAllUnconnectedUser(currentUser.getUserId());
        return ResponseEntity.ok(ResponseBuilder.success(users, "Non-connected users fetched successfully"));
    }

    @GetMapping("connected")
    public ResponseEntity<ResponseDTO<List<UserConnectionDTO>>> getAllAcceptedUsers(@AuthenticationPrincipal User currentUser) {
        List<UserConnectionDTO> acceptedUsers = userConnectionService.getAllAcceptedRequest(currentUser.getUserId());
        return ResponseEntity.ok(ResponseBuilder.success(acceptedUsers, "Accepted connections fetched successfully"));
    }

    @GetMapping("all")
    public ResponseEntity<ResponseDTO<AllConnectionDTO>> getAllConnectedUsers(@AuthenticationPrincipal User currentUser) {
        AllConnectionDTO allConnectionDetails = AllConnectionDTO.builder().acceptedList(userConnectionService.getAllAcceptedRequest(currentUser.getUserId())).pendingList(userConnectionService.getAllPendingRequest(currentUser.getUserId())).sentList(userConnectionService.getAllSentRequest(currentUser.getUserId())).blockedList(userConnectionService.getAllBlockedRequest(currentUser.getUserId())).build();
        return ResponseEntity.ok(ResponseBuilder.success(allConnectionDetails, "All connection details fetched successfully"));
    }

    @PostMapping("add/{receiverid}")
    public ResponseEntity<ResponseDTO<Object>> addNewConnection(@AuthenticationPrincipal User currentUser, @PathVariable("receiverid") Long receiverId) {
        userConnectionService.addConnectionRequest(currentUser.getUserId(), receiverId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseBuilder.success("Connection request sent successfully"));
    }

    @PostMapping("blocked/{id}")
    public ResponseEntity<ResponseDTO<Object>> blockedConnection(@AuthenticationPrincipal User currentUser, @PathVariable("id") Long id) {
        userConnectionService.addBlockRequest(currentUser.getUserId(), id);
        return ResponseEntity.ok(ResponseBuilder.success("User blocked successfully"));
    }

    @PostMapping("update/{connectionid}/{status}")
    public ResponseEntity<ResponseDTO<Object>> updateConnectionStatus(@AuthenticationPrincipal User currentUser, @PathVariable("connectionid") Long connectionId, @PathVariable("status") String status) {
        userConnectionService.updateStatus(currentUser.getUserId(), connectionId, status);
        return ResponseEntity.ok(ResponseBuilder.success("Status of the connection updated"));
    }


}

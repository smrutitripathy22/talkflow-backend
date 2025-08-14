package com.talkflow.dto.userConnections;


import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class AllConnectionDTO {

    private List<UserConnectionDTO> acceptedList;
    private List<UserConnectionDTO> pendingList;
    private List<UserConnectionDTO> sentList;
    private List<UserConnectionDTO> blockedList;
}

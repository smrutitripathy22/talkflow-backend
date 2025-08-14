package com.talkflow.dto.userConnections;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserConnectionDTO {
    private Long connectionId;

    private Long userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;

    private String status;


}

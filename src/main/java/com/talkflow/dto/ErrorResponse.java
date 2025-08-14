package com.talkflow.dto;

import lombok.*;

@Data
@Builder
@Setter
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private int status;
    private long timestamp;
}

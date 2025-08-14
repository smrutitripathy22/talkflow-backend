package com.talkflow.dto;


import lombok.Data;

@Data
public class ResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
    private long timestamp;
}

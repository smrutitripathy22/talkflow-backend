package com.talkflow.exception;

public class CustomEmailExistException extends RuntimeException {
    public CustomEmailExistException(String message) {
        super(message);
    }
}

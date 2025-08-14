package com.talkflow.util;

import com.talkflow.dto.ResponseDTO;

import java.util.Collections;

public class ResponseBuilder {


    public static <T> ResponseDTO<T> success(T data, String message) {
        ResponseDTO<T> response = new ResponseDTO<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }


    public static ResponseDTO<Object> success(String message) {
        ResponseDTO<Object> response = new ResponseDTO<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(Collections.emptyList());
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }


    public static ResponseDTO<Object> error(String errorMessage) {
        ResponseDTO<Object> response = new ResponseDTO<>();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setData(Collections.emptyList());
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}

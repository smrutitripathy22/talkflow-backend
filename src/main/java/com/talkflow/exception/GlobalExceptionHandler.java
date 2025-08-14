package com.talkflow.exception;

import com.talkflow.dto.ResponseDTO;
import com.talkflow.util.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Object>> handleGeneralException(Exception ex) {
        log.error("Exception Occurred: {}", ex.getMessage());
        return new ResponseEntity<>(ResponseBuilder.error(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation failed: {}", ex.getMessage());

        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getAllErrors().forEach(error -> {
            errorMessage.append(((FieldError) error).getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
        });

        return new ResponseEntity<>(ResponseBuilder.error(errorMessage.toString().trim()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CustomEmailExistException.class)
    public ResponseEntity<ResponseDTO<Object>> handleDuplicateEmailException(Exception ex) {
        log.error("Email Conflict: {}", ex.getMessage());
        return new ResponseEntity<>(ResponseBuilder.error(ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseDTO<Object>> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        return new ResponseEntity<>(ResponseBuilder.error("Invalid username or password"), HttpStatus.UNAUTHORIZED);
    }
}

package com.parkai.backend.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.parkai.backend.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(
            TooManyRequestsException ex
    ) {

        ErrorResponse error = new ErrorResponse(
        "TOO_MANY_REQUESTS",
        ex.getMessage(),
        HttpStatus.TOO_MANY_REQUESTS.value(),
        LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error);
    }
}
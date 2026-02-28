package com.live.locationtracker.exception;

import com.live.locationtracker.constant.GeneralConstant;
import com.live.locationtracker.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String mainMessage = errors.values().stream().findFirst().orElse("Validation failed");

        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
//        response.setStatusMessage(mainMessage);
        response.setStatusMessage(GeneralConstant.FAILURE);
        response.setData(errors);
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleAllExceptions(Exception ex) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setStatusMessage(GeneralConstant.FAILURE);
        response.setData(ex.getMessage() != null ? ex.getMessage() : "Internal server error");
        return response;
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<String> handleBadCredentials(BadCredentialsException ex) {
        return ApiResponse.error(401, GeneralConstant.FAILURE, "Invalid credentials");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<String> handleDisabled(DisabledException ex) {
        return ApiResponse.error(403, GeneralConstant.FAILURE, "Account is disabled");
    }
}

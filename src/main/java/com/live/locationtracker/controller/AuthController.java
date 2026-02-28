package com.live.locationtracker.controller;

import com.live.locationtracker.requestdto.*;
import com.live.locationtracker.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

public interface AuthController {
    ApiResponse<String> signup(@Valid @RequestBody SignupRequest request);

    ApiResponse<Object> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpSession session);

    ApiResponse<Object> login(@Valid @RequestBody LoginRequest request, HttpSession session);

    ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request);

    ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request);

    ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request);

    ApiResponse<Object> refreshToken(@Valid @RequestBody TokenDTO tokenDTO, HttpSession session);
}

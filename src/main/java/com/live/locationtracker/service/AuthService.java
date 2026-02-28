package com.live.locationtracker.service;

import com.live.locationtracker.requestdto.*;
import com.live.locationtracker.response.ApiResponse;
import com.live.locationtracker.model.User;
import jakarta.servlet.http.HttpSession;

public interface AuthService {
    ApiResponse<String> signup(SignupRequest request);

    ApiResponse<Object> verifyOtpAndCreateUser(VerifyOtpRequest request, HttpSession session);

    ApiResponse<Object> login(LoginRequest request, HttpSession session);

    ApiResponse<String> forgotPassword(ForgotPasswordRequest request);

    ApiResponse<String> resetPassword(ResetPasswordRequest request);

    ApiResponse<String> changePassword(String email, ChangePasswordRequest request);

    ApiResponse<Object> refreshToken(TokenDTO tokenDTO, HttpSession session);

    ApiResponse<Object> googleLogin(String code, HttpSession session);

    User getUser(String username);
}

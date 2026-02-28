package com.live.locationtracker.controller.impl;

import com.live.locationtracker.config.UserContextHolder;
import com.live.locationtracker.constant.GeneralConstant;
import com.live.locationtracker.controller.AuthController;
import com.live.locationtracker.requestdto.*;
import com.live.locationtracker.response.ApiResponse;
import com.live.locationtracker.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping(GeneralConstant.AUTH_BASE)
@Validated
public class AuthControllerImpl implements AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthControllerImpl.class);

    @Autowired
    private AuthService authService;

    @Override
    @PostMapping("/signup")
    public ApiResponse<String> signup(@RequestBody SignupRequest request) {
        logger.info("Signup request received for email: {}", request.getEmail());
        ApiResponse<String> response = authService.signup(request);
        logger.info("Signup response for email {}: {}", request.getEmail(), response.getStatusMessage());
        return response;
    }

    @Override
    @PostMapping("/verify-otp")
    public ApiResponse<Object> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpSession session) {
        logger.info("Verify OTP request received for email: {}", request.getEmail());
        ApiResponse<Object> response = authService.verifyOtpAndCreateUser(request, session);
        logger.info("Verify OTP response for email {}: {}", request.getEmail(), response.getStatusMessage());
        return response;
    }

    @Override
    @PostMapping("/login")
    public ApiResponse<Object> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        logger.info("Login request received for email: {}", request.getEmail());
        ApiResponse<Object> response = authService.login(request, session);
        logger.info("Login response for email {}: {}", request.getEmail(), response.getStatusMessage());
        return response;
    }

    @Override
    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Forgot password request received for email: {}", request.getEmail());
        ApiResponse<String> response = authService.forgotPassword(request);
        logger.info("Forgot password response for email {}: {}", request.getEmail(), response.getStatusMessage());
        return response;
    }

    @Override
    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        logger.info("Reset password request received");
        ApiResponse<String> response = authService.resetPassword(request);
        logger.info("Reset password response: {}", response.getStatusMessage());
        return response;
    }

    @Override
    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String email = UserContextHolder.getUserDto().getEmail();
        logger.info("Change password request received for email: {}", email);
        ApiResponse<String> response = authService.changePassword(email, request);
        logger.info("Change password response for email {}: {}", email, response.getStatusMessage());
        return response;
    }

    @Override
    @PostMapping("/refresh")
    public ApiResponse<Object> refreshToken(TokenDTO tokenDTO, HttpSession session) {
        logger.info("Refresh token request received");
        ApiResponse<Object> response = authService.refreshToken(tokenDTO, session);
        logger.info("Refresh token response: {}", response.getStatusMessage());
        return response;
    }

    @PostMapping("/google-login")
    public ApiResponse<Object> googleLogin(String code, HttpSession session) {
        logger.info("Google login request received");
        ApiResponse<Object> response = authService.googleLogin(code, session);
        logger.info("Google login response: {}", response.getStatusMessage());
        return response;
    }
}

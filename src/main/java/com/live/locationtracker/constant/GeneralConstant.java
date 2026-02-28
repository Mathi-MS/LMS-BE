package com.live.locationtracker.constant;

public class GeneralConstant {
    // Shared Strings
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    // Auth Endpoints
    public static final String AUTH_BASE = "/api/auth";
    public static final String USER_SIGNUP = "/signup";
    public static final String VERIFY_OTP = "/verify-otp";
    public static final String LOGIN = "/login";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String RESET_PASSWORD = "/reset-password";
    public static final String CHANGE_PASSWORD = "/change-password";
    public static final String REFRESH = "/refresh";
    public static final String GOOGLE_LOGIN = "/google-login";

    // JWT Claims
    public static final String EMAIL = "email";
    public static final String USERID = "id";
    public static final String MOBILE = "mobile";
    public static final String ROLE = "roll";

    // Response Messages
    public static final String OTP_SENT = "OTP sent to your email";
    public static final String PASSWORD_RESET_LINK_SENT = "Password reset link sent to your email";
    public static final String PASSWORD_RESET_SUCCESS = "Password has been reset successfully";
    public static final String PASSWORD_CHANGE_SUCCESS = "Password changed successfully";
    public static final String ALREADY_REGISTERED = "Hi %s, you have already registered. Please sign in.";

    // Integer Constants
    public static final int SUCCESS_CODE = 200;
    public static final int CREATED_CODE = 201;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
    public static final int NOT_FOUND_CODE = 404;
    public static final int INTERNAL_SERVER_ERROR_CODE = 500;

    public static final int OTP_LENGTH = 6;
    public static final int OTP_EXPIRY_MINUTES = 5;

    public static final String SUCCESS = "Success";
    public static final String FAILURE = "Failure";

    private GeneralConstant() {
    }
}

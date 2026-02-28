package com.live.locationtracker.service.impl;

import com.live.locationtracker.constant.GeneralConstant;
import com.live.locationtracker.model.PasswordResetToken;
import com.live.locationtracker.model.User;
import com.live.locationtracker.repository.PasswordResetRepository;
import com.live.locationtracker.repository.UserRepository;
import com.live.locationtracker.requestdto.*;
import com.live.locationtracker.response.ApiResponse;
import com.live.locationtracker.responseDto.AuthResponse;
import com.live.locationtracker.responseDto.UserDTO;
import com.live.locationtracker.service.AuthService;
import com.live.locationtracker.util.EmailService;
import com.live.locationtracker.util.JWTUtils;
import com.live.locationtracker.util.OTPUtils;
import com.live.locationtracker.util.RedisService;
import jakarta.servlet.http.HttpSession;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";


    @Autowired
    private UserRepository userRepository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    private RedisService redisService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ApiResponse<String> signup(SignupRequest request) {
        logger.info("Processing signup for email: {}", request.getEmail());
        try {
            User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (existingUser != null) {
                String fullName = (existingUser.getFirstName() != null ? existingUser.getFirstName() : "") +
                        (existingUser.getLastName() != null ? " " + existingUser.getLastName() : "");
                String message = String.format(GeneralConstant.ALREADY_REGISTERED, fullName.trim());
                logger.warn("User already registered with email: {}", request.getEmail());
                return ApiResponse.error(409, "Failure", "User already registered");

            }

            String otp = OTPUtils.generateOtp();
            redisService.saveOtp(request.getEmail(), otp, GeneralConstant.OTP_EXPIRY_MINUTES);
            emailService.sendOtp(request.getEmail(), otp);
            logger.info("OTP sent to email: {}", request.getEmail());
            return ApiResponse.success(GeneralConstant.OTP_SENT);
        } catch (Exception e) {
            logger.error("Error during signup for email {}: {}", request.getEmail(), e.getMessage());
            return ApiResponse.error(GeneralConstant.INTERNAL_SERVER_ERROR_CODE, "Failure", "Failed to send OTP: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<Object> verifyOtpAndCreateUser(VerifyOtpRequest request, HttpSession session) {
        logger.info("Verifying OTP for email: {}", request.getEmail());
        String cachedOtp = redisService.getOtp(request.getEmail());
        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            logger.error("Invalid or expired OTP for email: {}", request.getEmail());
            return ApiResponse.error(400, "Failure", "Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            logger.info("Creating new user for email: {}", request.getEmail());
            user = new User();
            user.setEmail(request.getEmail());
            user.setRoles(List.of("USER"));
            user.setActive(true);
            user.setDeleted(false);
            user.setPasswordSet(false);
            user.setCreatedBy(request.getEmail());
            user.setUpdatedBy(request.getEmail());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        redisService.deleteOtp(request.getEmail());
        AuthResponse authResponse = generateAuthResponse(user, session);
        logger.info("OTP verified and auth response generated for email: {}", request.getEmail());
        return ApiResponse.success(authResponse);
    }

    @Override
    public ApiResponse<Object> login(LoginRequest request, HttpSession session) {
        logger.info("Login attempt for email: {}", request.getEmail());
        try {
            AuthResponse authResponse = authenticate(request.getEmail(), request.getPassword(), session);
            logger.info("Login successful for email: {}", request.getEmail());
            return ApiResponse.success(authResponse);
        } catch (Exception e) {
            logger.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ApiResponse.error(401, "Failure", "Invalid email or password");
        }
    }

    public AuthResponse authenticate(String username, String password, HttpSession session) {
        final Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = getUser(username);
        return generateAuthResponse(user, session);
    }

    private AuthResponse generateAuthResponse(User user, HttpSession session) {
        String token = jwtUtils.generateToken(user, session);
        String refreshToken = jwtUtils.refreshToken(token, user);
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        userDTO.setPasswordSet(user.isPasswordSet()); // Explicitly set if mapping missed it
        return new AuthResponse(token, refreshToken, userDTO);
    }

    @Override
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request) {
        logger.info("Processing forgot password for email: {}", request.getEmail());
        String token = UUID.randomUUID().toString();

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            logger.error("User not found for forgot password email: {}", request.getEmail());
            return ApiResponse.error(400, "Failure", "User not found");
        }

        PasswordResetToken reset = new PasswordResetToken();
        reset.setEmail(user.getEmail());
        reset.setToken(token);
        reset.setCreatedAt(LocalDateTime.now());
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        reset.setUsed(false);
        passwordResetRepository.save(reset);

        String resetLink = frontendUrl + "/resetPassword?token=" + token;
        emailService.sendResetLink(user.getEmail(), resetLink);
        logger.info("Password reset link sent to: {}", user.getEmail());
        return ApiResponse.success(GeneralConstant.PASSWORD_RESET_LINK_SENT);
    }

    @Override
    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        logger.info("Processing password reset");
        PasswordResetToken resetToken = passwordResetRepository.findByToken(request.getToken());

        if (resetToken == null || resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Expired or invalid reset token used");
            return ApiResponse.error(400, "Failure", "Token is expired or invalid");
        }

        User user = userRepository.findByEmail(resetToken.getEmail()).orElse(null);
        if (user == null) {
            logger.error("User not found for reset token: {}", resetToken.getEmail());
            return ApiResponse.error(400, "Failure", "User not found");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            logger.error("Passwords do not match during reset for: {}", resetToken.getEmail());
            return ApiResponse.error(400, "Failure", "Passwords do not match");
        }

        resetToken.setUsed(true);
        passwordResetRepository.save(resetToken);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        user.setUpdatedBy(user.getEmail());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Password reset successfully for: {}", user.getEmail());
        return ApiResponse.success(GeneralConstant.PASSWORD_RESET_SUCCESS);
    }

    @Override
    public ApiResponse<String> changePassword(String email, ChangePasswordRequest request) {
        logger.info("Processing password change for: {}", email);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            logger.error("User not found for change password email: {}", email);
            return ApiResponse.error(400, "Failure", "User not found");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            logger.error("Incorrect old password for: {}", email);
            return ApiResponse.error(400, "Failure", "Incorrect old password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            logger.error("New passwords do not match for: {}", email);
            return ApiResponse.error(400, "Failure", "New passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        user.setUpdatedBy(email);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Password changed successfully for: {}", email);
        return ApiResponse.success(GeneralConstant.PASSWORD_CHANGE_SUCCESS);
    }

    @Override
    public ApiResponse<Object> refreshToken(TokenDTO tokenDTO, HttpSession session) {
        logger.info("Processing token refresh");
        try {
            String email = jwtUtils.getUsernameFromToken(tokenDTO.getRefreshToken());
            User user = getUser(email);
            if (user == null) {
                return ApiResponse.error(400, "Failure", "User not found");
            }
            AuthResponse authResponse = generateAuthResponse(user, session);
            logger.info("Token refreshed for: {}", email);
            return ApiResponse.success(authResponse);
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return ApiResponse.error(401, "Failure", "Invalid refresh token");
        }
    }

    @Override
    public ApiResponse<Object> googleLogin(String code, HttpSession session) {

        logger.info("Processing Google SSO via Authorization Code flow");

        if (code == null || code.isBlank()) {
            return ApiResponse.error(400, "Failure", "Missing authorization code");
        }

        try {

            // Step 1: Exchange authorization code for access token
            String accessToken = getAccessTokenFromCode(code);

            // Step 2: Fetch user info using access token
            Map<String, Object> userInfo = getUserInfo(accessToken);

            String email = (String) userInfo.get("email");
            String firstName = (String) userInfo.get("given_name");
            String lastName = (String) userInfo.get("family_name");
            Boolean emailVerified = (Boolean) userInfo.get("email_verified");

            if (email == null || emailVerified == null || !emailVerified) {
                return ApiResponse.error(401, "Failure", "Google email not verified");
            }

            // Step 3: Create or fetch user
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                logger.info("Creating new user via Google SSO: {}", email);

                user = new User();
                user.setEmail(email);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setRoles(List.of("USER"));
                user.setActive(true);
                user.setDeleted(false);
                user.setPasswordSet(false);
                user.setCreatedBy(email);
                user.setUpdatedBy(email);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                userRepository.save(user);
            }

            // Step 4: Generate your JWT
            AuthResponse authResponse = generateAuthResponse(user, session);

            return ApiResponse.success(authResponse);

        } catch (Exception e) {
            logger.error("Google OAuth failed: {}", e.getMessage());
            return ApiResponse.error(500, "Failure", "Google authentication failed");
        }
    }

    private String getAccessTokenFromCode(String code) {

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        logger.info("Using redirect URI: {}", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                TOKEN_URL,
                request,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null
                || response.getBody().get("access_token") == null) {

            throw new RuntimeException("Failed to retrieve access token from Google");
        }

        return (String) response.getBody().get("access_token");
    }

    private Map<String, Object> getUserInfo(String accessToken) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                USER_INFO_URL,
                HttpMethod.GET,
                entity,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to fetch user info from Google");
        }

        return response.getBody();
    }

//    @Override
//    public ApiResponse<Object> googleLogin(Map<String, String> googleRequest, HttpSession session) {
//        logger.info("Processing Google SSO login via ID Token verification");
//        String idTokenString = googleRequest.get("idToken");
//
//        if (idTokenString == null || idTokenString.isEmpty()) {
//            logger.error("Missing Google ID Token");
//            return ApiResponse.error(400, "Failure", "Missing ID Token");
//        }
//
//        try {
//            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
//                    new GsonFactory())
//                    .setAudience(Collections.singletonList(clientId))
//                    .build();
//
//            GoogleIdToken idToken = verifier.verify(idTokenString);
//            if (idToken == null) {
//                logger.error("Invalid Google ID Token");
//                return ApiResponse.error(401, "Failure", "Invalid ID Token");
//            }
//
//            GoogleIdToken.Payload payload = idToken.getPayload();
//            String email = payload.getEmail();
//            String firstName = (String) payload.get("given_name");
//            String lastName = (String) payload.get("family_name");
//
//            User user = userRepository.findByEmail(email).orElse(null);
//            if (user == null) {
//                logger.info("Creating new user for Google SSO: {}", email);
//                user = new User();
//                user.setEmail(email);
//                user.setFirstName(firstName);
//                user.setLastName(lastName);
//                user.setRoles(List.of("USER"));
//                user.setActive(true);
//                user.setDeleted(false);
//                user.setPasswordSet(false);
//                user.setCreatedBy(email);
//                user.setUpdatedBy(email);
//                user.setCreatedAt(LocalDateTime.now());
//                user.setUpdatedAt(LocalDateTime.now());
//                userRepository.save(user);
//            }
//
//            AuthResponse authResponse = generateAuthResponse(user, session);
//            logger.info("Google SSO successful for: {}", email);
//            return ApiResponse.success(authResponse);
//        } catch (Exception e) {
//            logger.error("Google SSO login failed: {}", e.getMessage());
//            return ApiResponse.error(500, "Failure", "SSO verification failed");
//        }
//    }

    @Override
    public User getUser(String username) {
        return userRepository.findByEmail(username).orElse(null);
    }


//    public Map<String, Object> exchangeCodeForUser(String code) throws Exception {
//
//        RestTemplate restTemplate = new RestTemplate();
//
//        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
//        tokenRequest.add("code", code);
//        tokenRequest.add("client_id", clientId);
//        tokenRequest.add("client_secret", clientSecret);
//        tokenRequest.add("redirect_uri", redirectUri);
//        tokenRequest.add("grant_type", "authorization_code");
//
//        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
//                TOKEN_URL,
//                new HttpEntity<>(tokenRequest, new HttpHeaders()),
//                Map.class
//        );
//
//        String accessToken = (String) tokenResponse.getBody().get("access_token");
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//
//        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
//                USER_INFO_URL,
//                HttpMethod.GET,
//                new HttpEntity<>(headers),
//                Map.class
//        );
//
//        return userInfoResponse.getBody();
//    }
}

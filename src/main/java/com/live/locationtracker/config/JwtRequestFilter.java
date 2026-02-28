package com.live.locationtracker.config;

import com.live.locationtracker.model.User;
import com.live.locationtracker.repository.UserRepository;
import com.live.locationtracker.responseDto.UserContextDTO;
import com.live.locationtracker.service.AuthService;
import com.live.locationtracker.requestdto.DecodeTokenDTO;
import com.live.locationtracker.constant.GeneralConstant;
import com.live.locationtracker.util.JWTUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Autowired
    private JWTUtils jwtTokenUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (shouldSkipFilter(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwtToken = extractJwtToken(request.getHeader(GeneralConstant.AUTHORIZATION));
            if (jwtToken != null) {
                processJwtToken(jwtToken, request);
            } else {
                throw new ServletException("JWT Token does not begin with Bearer String");
            }

            filterChain.doFilter(request, response);
        } catch (ServletException servletException) {
            handleException(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, servletException.getMessage());
        } catch (Exception exception) {
            handleException(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        } finally {
            UserContextHolder.clear();
        }
    }

    private void handleException(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("statusCode", status);
        errorResponse.put("statusMessage", message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }

    public boolean shouldSkipFilter(HttpServletRequest request) {
        return checker(request) || StringUtils.isBlank(request.getHeader(GeneralConstant.AUTHORIZATION));
    }

    private String extractJwtToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(GeneralConstant.BEARER)) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    private void processJwtToken(String jwtToken, HttpServletRequest request) throws ServletException {
        try {
            String username = getUsernameFromJwtToken(jwtToken);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = authService.getUser(username);
                if (user != null) {
                    UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(),
                            user.getPassword(), new ArrayList<>());
                    if (Boolean.TRUE.equals(jwtTokenUtil.validateToken(jwtToken, userDetails))) {
                        authenticateWithJwtToken(userDetails, request);
                    } else {
                        throw new ServletException("Access Token is invalid");
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            throw new ServletException("Access Token has expired");
        } catch (Exception e) {
            throw new ServletException(e.getMessage());
        }
    }

    private String getUsernameFromJwtToken(String jwtToken) throws Exception {
        DecodeTokenDTO dto = extractTokenDto(jwtToken);
        if (dto.getSub() != null) {
            Optional<User> user = userRepository.findByEmail(dto.getSub());
            user.ifPresent(u -> {
                UserContextDTO userDTO = new UserContextDTO();
                userDTO.setId(u.getId());
                userDTO.setUserName(u.getUsername());
                userDTO.setFirstName(u.getFirstName());
                userDTO.setLastName(u.getLastName());
                userDTO.setEmail(u.getEmail());
                userDTO.setMobileNumber(u.getMobileNumber());
                userDTO.setDob(u.getDob());
                UserContextHolder.setUserDto(userDTO);
            });
            return dto.getSub();
        }
        return null;
    }

    private DecodeTokenDTO extractTokenDto(String jwtToken) throws Exception {
        String[] split = jwtToken.split("\\.");
        if (split.length < 2)
            throw new ServletException("Invalid JWT token");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(split[1]));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return objectMapper.readValue(payload, DecodeTokenDTO.class);
    }

    private void authenticateWithJwtToken(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = jwtTokenUtil.getAuthentication(userDetails);
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private boolean checker(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Handle common variations and ensure it works with/without trailing slashes
        List<String> startWithPaths = List.of(
                "/actuator/health",
                "/v3/api-docs",
                "/configuration",
                "/swagger-ui",
                "/signup",
                "/login",
                "/verify-otp",
                "/forgot-password",
                "/reset-password",
                "/favicon.ico");

        for (String path : startWithPaths) {
            if (uri.startsWith(path) || (uri + "/").startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}

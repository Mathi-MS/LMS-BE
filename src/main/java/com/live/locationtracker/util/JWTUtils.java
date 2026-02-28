package com.live.locationtracker.util;

import com.live.locationtracker.constant.GeneralConstant;
import com.live.locationtracker.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Component
public class JWTUtils implements Serializable {
    private static final long serialVersionUID = -2550185165626007488L;

    public static final long JWT_TOKEN_VALIDITY = 12L * 60L * 60L * 1000L;
    public static final long JWT_REFRESH_TOKEN_VALIDITY = 13L * 60L * 60L * 1000L;

    @Value("${jwt.secret}")
    public String secret;

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getEmail(String token) {
        return extractValueFromToken(token, GeneralConstant.EMAIL, secret);
    }

    public String getId(String token) {
        return extractValueFromTokenForUserId(token, GeneralConstant.USERID, secret);
    }

    private String extractValueFromTokenForUserId(String token, String id, String secret) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().get(id).toString();
    }

    private String extractValueFromToken(String token, String email, String secret) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().get(email).toString();
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String generateToken(User user, HttpSession session) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(GeneralConstant.EMAIL, user.getEmail());
        claims.put(GeneralConstant.MOBILE, user.getMobileNumber());
        claims.put(GeneralConstant.USERID, user.getId());
        claims.put(GeneralConstant.ROLE, user.getApplicationRole().getRoleName());
        session.setAttribute("getId", user.getId());
        try {
            return getAccessToken(claims, user.getEmail());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getAccessToken(Map<String, Object> claims, String userName) {
        try {
            if (claims == null) {
                claims = Map.of(); // empty map if null
            }

            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userName)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                    .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                    .compact();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    public String refreshToken(String token, User user) {
        final Claims claims = extractAllClaims(token);
        claims.setIssuedAt(new Date(System.currentTimeMillis()));
        claims.setExpiration(new Date(System.currentTimeMillis() + JWT_REFRESH_TOKEN_VALIDITY));
        claims.put(GeneralConstant.EMAIL, user.getEmail());
        claims.put(GeneralConstant.MOBILE, user.getMobileNumber());
        claims.put(GeneralConstant.USERID, user.getId());
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret.getBytes()).compact();
    }

    public UsernamePasswordAuthenticationToken getAuthentication(final UserDetails userDetails) {
        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }
}

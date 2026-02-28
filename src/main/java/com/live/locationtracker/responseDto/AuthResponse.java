package com.live.locationtracker.responseDto;

import lombok.Data;

@Data
public class AuthResponse {

    private String token;
    private String refreshToken;
    private UserDTO users;

    public AuthResponse(String token, String refreshToken, UserDTO users) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.users = users;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserDTO getUsers() {
        return users;
    }

    public void setUsers(UserDTO users) {
        this.users = users;
    }
}

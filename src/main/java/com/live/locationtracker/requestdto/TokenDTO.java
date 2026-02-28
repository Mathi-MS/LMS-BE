package com.live.locationtracker.requestdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenDTO {
    @NotBlank(message = "RefreshToken is required")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

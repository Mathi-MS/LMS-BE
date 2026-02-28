package com.live.locationtracker.requestdto;

import com.live.locationtracker.util.StrictEmail;
import com.live.locationtracker.util.StrictPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @StrictEmail
    private String email;

    @NotBlank(message = "Password is required")
    @StrictPassword
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

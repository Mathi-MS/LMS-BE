package com.live.locationtracker.requestdto;

import com.live.locationtracker.util.StrictEmail;
import com.live.locationtracker.util.StrictPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank(message = "FullName is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @StrictEmail
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}

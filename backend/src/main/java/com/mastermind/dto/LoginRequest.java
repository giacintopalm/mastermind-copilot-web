package com.mastermind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank(message = "Nickname is required")
    @Size(min = 3, max = 20, message = "Nickname must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Nickname can only contain letters, numbers, underscores and hyphens")
    private String nickname;

    public LoginRequest() {
    }

    public LoginRequest(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

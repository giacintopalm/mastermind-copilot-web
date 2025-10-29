package com.mastermind.dto;

public class LoginResponse {
    private boolean success;
    private String sessionId;
    private String message;
    private String nickname;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String sessionId, String nickname, String message) {
        this.success = success;
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.message = message;
    }

    public static LoginResponse success(String sessionId, String nickname) {
        return new LoginResponse(true, sessionId, nickname, "Login successful");
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(false, null, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

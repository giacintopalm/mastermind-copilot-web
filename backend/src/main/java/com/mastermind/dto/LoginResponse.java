package com.mastermind.dto;

/**
 * Data Transfer Object for login responses.
 * On success, carries the session ID and nickname; on failure, carries an error message.
 */
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

    /**
     * Create a successful login response.
     *
     * @param sessionId the newly created session ID
     * @param nickname  the player's display name
     * @return a {@code LoginResponse} with {@code success = true}
     */
    public static LoginResponse success(String sessionId, String nickname) {
        return new LoginResponse(true, sessionId, nickname, "Login successful");
    }

    /**
     * Create a failed login response.
     *
     * @param message a human-readable explanation of why login failed
     * @return a {@code LoginResponse} with {@code success = false}
     */
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

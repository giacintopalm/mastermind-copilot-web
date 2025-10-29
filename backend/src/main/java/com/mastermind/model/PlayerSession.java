package com.mastermind.model;

import java.time.LocalDateTime;

public class PlayerSession {
    private String sessionId;
    private String nickname;
    private LocalDateTime connectedAt;
    private PlayerStatus status;

    public PlayerSession() {
    }

    public PlayerSession(String sessionId, String nickname) {
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.connectedAt = LocalDateTime.now();
        this.status = PlayerStatus.AVAILABLE;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public enum PlayerStatus {
        AVAILABLE,
        IN_GAME,
        AWAY
    }
}

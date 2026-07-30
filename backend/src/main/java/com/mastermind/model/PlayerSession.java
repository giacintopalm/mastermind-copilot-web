package com.mastermind.model;

import java.time.LocalDateTime;

/**
 * Represents an active player session in the multiplayer lobby.
 * Tracks the player's connection time, last activity, and current status
 * so that idle players can be automatically removed.
 */
public class PlayerSession {
    private String sessionId;
    private String nickname;
    private LocalDateTime connectedAt;
    private LocalDateTime lastActivity;
    private PlayerStatus status;

    public PlayerSession() {
    }

    public PlayerSession(String sessionId, String nickname) {
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.connectedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
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

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    /**
     * Update the last-activity timestamp to the current time.
     * Call this whenever the player performs an action so they are not
     * evicted by the inactivity cleanup scheduler.
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Possible activity states for a player session.
     */
    public enum PlayerStatus {
        AVAILABLE,
        IN_GAME,
        AWAY
    }
}

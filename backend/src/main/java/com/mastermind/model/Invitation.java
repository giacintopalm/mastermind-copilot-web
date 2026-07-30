package com.mastermind.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a player-to-player game invitation.
 * An invitation starts as PENDING and transitions to ACCEPTED, DECLINED,
 * EXPIRED, or CANCELLED depending on recipient action or time-out.
 */
public class Invitation {
    private String invitationId;
    private String fromNickname;
    private String toNickname;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public Invitation() {
    }

    public Invitation(String fromNickname, String toNickname) {
        this.invitationId = UUID.randomUUID().toString();
        this.fromNickname = fromNickname;
        this.toNickname = toNickname;
        this.status = InvitationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public String getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(String invitationId) {
        this.invitationId = invitationId;
    }

    public String getFromNickname() {
        return fromNickname;
    }

    public void setFromNickname(String fromNickname) {
        this.fromNickname = fromNickname;
    }

    public String getToNickname() {
        return toNickname;
    }

    public void setToNickname(String toNickname) {
        this.toNickname = toNickname;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    /**
     * Possible lifecycle states for an invitation.
     */
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED,
        CANCELLED
    }
}

package com.mastermind.dto;

/**
 * Data Transfer Object for sending a game invitation to another player.
 */
public class InvitationRequest {
    private String toNickname;

    public InvitationRequest() {
    }

    public InvitationRequest(String toNickname) {
        this.toNickname = toNickname;
    }

    public String getToNickname() {
        return toNickname;
    }

    public void setToNickname(String toNickname) {
        this.toNickname = toNickname;
    }
}

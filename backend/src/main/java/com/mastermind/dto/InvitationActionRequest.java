package com.mastermind.dto;

/**
 * Data Transfer Object for accepting or declining a game invitation.
 * Contains the invitation ID and a flag indicating the player's decision.
 */
public class InvitationActionRequest {
    private String invitationId;
    private boolean accept;

    public InvitationActionRequest() {
    }

    public InvitationActionRequest(String invitationId, boolean accept) {
        this.invitationId = invitationId;
        this.accept = accept;
    }

    public String getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(String invitationId) {
        this.invitationId = invitationId;
    }

    public boolean isAccept() {
        return accept;
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }
}

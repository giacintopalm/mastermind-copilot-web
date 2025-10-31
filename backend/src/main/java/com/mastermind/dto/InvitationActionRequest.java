package com.mastermind.dto;

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

package com.mastermind.dto;

public class InvitationResponse {
    private String invitationId;
    private String fromNickname;
    private String toNickname;
    private String status;
    private String message;

    public InvitationResponse() {
    }

    public InvitationResponse(String invitationId, String fromNickname, String toNickname, String status) {
        this.invitationId = invitationId;
        this.fromNickname = fromNickname;
        this.toNickname = toNickname;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

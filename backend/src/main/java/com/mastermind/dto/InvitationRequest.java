package com.mastermind.dto;

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

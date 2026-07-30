package com.mastermind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessageRequest {

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("to")
    private String to;

    @JsonProperty("message")
    private String message;

    public ChatMessageRequest() {}

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

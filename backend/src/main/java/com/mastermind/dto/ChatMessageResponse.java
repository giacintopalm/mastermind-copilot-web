package com.mastermind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessageResponse {

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private long timestamp;

    public ChatMessageResponse() {}

    public ChatMessageResponse(String nickname, String message, long timestamp) {
        this.nickname = nickname;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

package com.mastermind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for error responses.
 */
public class ErrorResponse {
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private long timestamp;

    public ErrorResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ErrorResponse(String error, String message) {
        this();
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("ErrorResponse{error='%s', message='%s'}", error, message);
    }
}
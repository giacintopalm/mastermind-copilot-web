package com.mastermind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * Data Transfer Object for creating a new game.
 */
public class CreateGameRequest {
    
    @Min(value = 1, message = "Slot count must be at least 1")
    @JsonProperty("slotCount")
    private Integer slotCount;
    
    @JsonProperty("secret")
    private List<String> secret;

    public CreateGameRequest() {
        // Default constructor for Jackson
    }

    public CreateGameRequest(Integer slotCount) {
        this.slotCount = slotCount;
    }

    public CreateGameRequest(Integer slotCount, List<String> secret) {
        this.slotCount = slotCount;
        this.secret = secret;
    }

    public Integer getSlotCount() {
        return slotCount;
    }

    public void setSlotCount(Integer slotCount) {
        this.slotCount = slotCount;
    }

    public List<String> getSecret() {
        return secret;
    }

    public void setSecret(List<String> secret) {
        this.secret = secret;
    }

    @Override
    public String toString() {
        return String.format("CreateGameRequest{slotCount=%d, hasCustomSecret=%s}", 
            slotCount, secret != null && !secret.isEmpty());
    }
}
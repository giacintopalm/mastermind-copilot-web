package com.mastermind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

/**
 * Data Transfer Object for creating a new game.
 */
public class CreateGameRequest {
    
    @Min(value = 1, message = "Slot count must be at least 1")
    @JsonProperty("slotCount")
    private Integer slotCount;

    public CreateGameRequest() {
        // Default constructor for Jackson
    }

    public CreateGameRequest(Integer slotCount) {
        this.slotCount = slotCount;
    }

    public Integer getSlotCount() {
        return slotCount;
    }

    public void setSlotCount(Integer slotCount) {
        this.slotCount = slotCount;
    }

    @Override
    public String toString() {
        return String.format("CreateGameRequest{slotCount=%d}", slotCount);
    }
}
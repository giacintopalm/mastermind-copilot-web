package com.mastermind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Data Transfer Object for submitting a guess in the Mastermind game.
 */
public class GuessRequest {
    
    @NotNull(message = "Colors are required")
    @Size(min = 1, message = "At least one color is required")
    @JsonProperty("colors")
    private List<String> colors;

    public GuessRequest() {
        // Default constructor for Jackson
    }

    public GuessRequest(List<String> colors) {
        this.colors = colors;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    @Override
    public String toString() {
        return String.format("GuessRequest{colors=%s}", colors);
    }
}
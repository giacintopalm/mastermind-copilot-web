package com.mastermind.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a single guess attempt in the Mastermind game.
 * Contains the guessed colors and the feedback received.
 */
public class Guess {
    
    @JsonProperty("slots")
    private List<Color> slots;
    
    @JsonProperty("feedback")
    private Feedback feedback;

    public Guess() {
        // Default constructor for Jackson
    }

    public Guess(List<Color> slots, Feedback feedback) {
        this.slots = slots;
        this.feedback = feedback;
    }

    public List<Color> getSlots() {
        return slots;
    }

    public void setSlots(List<Color> slots) {
        this.slots = slots;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return String.format("Guess{slots=%s, feedback=%s}", slots, feedback);
    }
}
package com.mastermind.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a complete guess attempt in the Mastermind game.
 * Contains the original guess made by the player and the feedback received.
 * This is used for game history and for suggestion algorithms that need
 * to find compatible guesses based on previous attempts.
 */
public class GuessAttempt {
    
    @JsonProperty("guess")
    private List<Color> guess;
    
    @JsonProperty("feedback")
    private Feedback feedback;

    public GuessAttempt() {
        // Default constructor for Jackson
    }

    public GuessAttempt(List<Color> guess, Feedback feedback) {
        this.guess = guess;
        this.feedback = feedback;
    }

    /**
     * Get the original guess that was made.
     * @return The list of colors guessed
     */
    public List<Color> getGuess() {
        return guess;
    }

    public void setGuess(List<Color> guess) {
        this.guess = guess;
    }

    /**
     * Get the feedback received for this guess.
     * @return The feedback containing exact and partial matches
     */
    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return String.format("GuessAttempt{guess=%s, feedback=%s}", guess, feedback);
    }
}
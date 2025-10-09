package com.mastermind.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the feedback for a guess in the Mastermind game.
 * Contains exact matches (correct color in correct position) and 
 * partial matches (correct color in wrong position).
 */
public class Feedback {
    
    @JsonProperty("exact")
    private int exact;
    
    @JsonProperty("partial") 
    private int partial;

    public Feedback() {
        // Default constructor for Jackson
    }

    public Feedback(int exact, int partial) {
        this.exact = exact;
        this.partial = partial;
    }

    public int getExact() {
        return exact;
    }

    public void setExact(int exact) {
        this.exact = exact;
    }

    public int getPartial() {
        return partial;
    }

    public void setPartial(int partial) {
        this.partial = partial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Feedback feedback = (Feedback) o;
        return exact == feedback.exact && partial == feedback.partial;
    }

    @Override
    public int hashCode() {
        return exact * 31 + partial;
    }

    @Override
    public String toString() {
        return String.format("Feedback{exact=%d, partial=%d}", exact, partial);
    }
}
package com.mastermind.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a complete Mastermind game session.
 * Contains the secret code, game history, current state, and metadata.
 */
public class Game {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("secret")
    private List<Color> secret;
    
    @JsonProperty("history")
    private List<GuessAttempt> history;
    
    @JsonProperty("gameOver")
    private boolean gameOver;
    
    @JsonProperty("won")
    private boolean won;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("slotCount")
    private int slotCount;

    public Game() {
        // Default constructor for Jackson
        this.id = UUID.randomUUID().toString();
        this.history = new ArrayList<>();
        this.gameOver = false;
        this.won = false;
        this.createdAt = LocalDateTime.now();
        this.slotCount = 4; // Default slot count
    }

    public Game(List<Color> secret, int slotCount) {
        this();
        this.secret = secret;
        this.slotCount = slotCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Color> getSecret() {
        return secret;
    }

    public void setSecret(List<Color> secret) {
        this.secret = secret;
    }

    public List<GuessAttempt> getHistory() {
        return history;
    }

    public void setHistory(List<GuessAttempt> history) {
        this.history = history;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public void setSlotCount(int slotCount) {
        this.slotCount = slotCount;
    }

    /**
     * Add a guess attempt to the game history and update game state.
     * @param guessAttempt The guess attempt (guess + feedback) to add
     */
    public void addGuessAttempt(GuessAttempt guessAttempt) {
        this.history.add(guessAttempt);
        
        // Check if game is won (all exact matches)
        if (guessAttempt.getFeedback().getExact() == slotCount) {
            this.won = true;
            this.gameOver = true;
        }
    }

    @Override
    public String toString() {
        return String.format("Game{id='%s', gameOver=%s, won=%s, historySize=%d}", 
                           id, gameOver, won, history.size());
    }
}
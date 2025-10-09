package com.mastermind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mastermind.model.Game;
import com.mastermind.model.GuessAttempt;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for game responses.
 * Excludes the secret from the response to prevent cheating.
 */
public class GameResponse {
    
    @JsonProperty("id")
    private String id;
    
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

    public GameResponse() {
        // Default constructor for Jackson
    }

    /**
     * Create a GameResponse from a Game entity, excluding the secret.
     */
    public static GameResponse fromGame(Game game) {
        GameResponse response = new GameResponse();
        response.id = game.getId();
        response.history = game.getHistory();
        response.gameOver = game.isGameOver();
        response.won = game.isWon();
        response.createdAt = game.getCreatedAt();
        response.slotCount = game.getSlotCount();
        return response;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public String toString() {
        return String.format("GameResponse{id='%s', gameOver=%s, won=%s, historySize=%d}", 
                           id, gameOver, won, history != null ? history.size() : 0);
    }
}
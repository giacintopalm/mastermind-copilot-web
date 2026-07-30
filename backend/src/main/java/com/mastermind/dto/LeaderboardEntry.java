package com.mastermind.dto;

/**
 * Data Transfer Object for a single leaderboard entry.
 * Aggregates a player's win count, total game count, and average guess count.
 */
public class LeaderboardEntry {
    private String nickname;
    private long wins;
    private long games;
    private double avgGuesses;

    public LeaderboardEntry(String nickname, long wins, long games, double avgGuesses) {
        this.nickname = nickname;
        this.wins = wins;
        this.games = games;
        this.avgGuesses = avgGuesses;
    }

    public String getNickname() { return nickname; }
    public long getWins() { return wins; }
    public long getGames() { return games; }
    public double getAvgGuesses() { return avgGuesses; }
}

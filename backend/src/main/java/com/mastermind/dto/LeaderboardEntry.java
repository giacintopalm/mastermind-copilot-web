package com.mastermind.dto;

public class LeaderboardEntry {
    private String nickname;
    private long wins;
    private long games;
    private double avgGuesses;
    private Integer bestGuesses;

    public LeaderboardEntry(String nickname, long wins, long games, double avgGuesses, Integer bestGuesses) {
        this.nickname = nickname;
        this.wins = wins;
        this.games = games;
        this.avgGuesses = avgGuesses;
        this.bestGuesses = bestGuesses;
    }

    public String getNickname() { return nickname; }
    public long getWins() { return wins; }
    public long getGames() { return games; }
    public double getAvgGuesses() { return avgGuesses; }
    public Integer getBestGuesses() { return bestGuesses; }
}

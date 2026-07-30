package com.mastermind.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a multiplayer game match between two players.
 * Each player chooses a secret code for the other to guess; the match
 * moves through SETUP → PLAYING → FINISHED states.
 */
public class GameMatch {
    private String matchId;
    private String player1Nickname;
    private String player2Nickname;
    private String player1GameId;
    private String player2GameId;
    private boolean player1Ready;
    private boolean player2Ready;
    private MatchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;

    public GameMatch() {
    }

    public GameMatch(String player1Nickname, String player2Nickname) {
        this.matchId = UUID.randomUUID().toString();
        this.player1Nickname = player1Nickname;
        this.player2Nickname = player2Nickname;
        this.player1Ready = false;
        this.player2Ready = false;
        this.status = MatchStatus.SETUP;
        this.createdAt = LocalDateTime.now();
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getPlayer1Nickname() {
        return player1Nickname;
    }

    public void setPlayer1Nickname(String player1Nickname) {
        this.player1Nickname = player1Nickname;
    }

    public String getPlayer2Nickname() {
        return player2Nickname;
    }

    public void setPlayer2Nickname(String player2Nickname) {
        this.player2Nickname = player2Nickname;
    }

    public String getPlayer1GameId() {
        return player1GameId;
    }

    public void setPlayer1GameId(String player1GameId) {
        this.player1GameId = player1GameId;
    }

    public String getPlayer2GameId() {
        return player2GameId;
    }

    public void setPlayer2GameId(String player2GameId) {
        this.player2GameId = player2GameId;
    }

    public boolean isPlayer1Ready() {
        return player1Ready;
    }

    public void setPlayer1Ready(boolean player1Ready) {
        this.player1Ready = player1Ready;
    }

    public boolean isPlayer2Ready() {
        return player2Ready;
    }

    public void setPlayer2Ready(boolean player2Ready) {
        this.player2Ready = player2Ready;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Check whether a given player is a participant in this match.
     *
     * @param nickname the nickname to test
     * @return {@code true} if the nickname matches player 1 or player 2
     */
    public boolean isPlayerInMatch(String nickname) {
        return player1Nickname.equals(nickname) || player2Nickname.equals(nickname);
    }

    /**
     * Get the nickname of the other player in this match.
     *
     * @param nickname the requesting player's nickname
     * @return the opponent's nickname, or {@code null} if the given nickname is not in this match
     */
    public String getOpponentNickname(String nickname) {
        if (player1Nickname.equals(nickname)) {
            return player2Nickname;
        } else if (player2Nickname.equals(nickname)) {
            return player1Nickname;
        }
        return null;
    }

    /**
     * Determine whether both players have submitted their secrets and are ready to play.
     *
     * @return {@code true} if both player 1 and player 2 are marked as ready
     */
    public boolean areBothPlayersReady() {
        return player1Ready && player2Ready;
    }

    /**
     * Lifecycle states of a multiplayer match.
     */
    public enum MatchStatus {
        SETUP,      // Players setting secrets
        PLAYING,    // Game in progress
        FINISHED    // Game completed
    }
}

package com.mastermind.dto;

public class GameMatchResponse {
    private String matchId;
    private String player1Nickname;
    private String player2Nickname;
    private String player1GameId;
    private String player2GameId;
    private boolean player1Ready;
    private boolean player2Ready;
    private String status;
    private String message;

    public GameMatchResponse() {
    }

    public GameMatchResponse(String matchId, String player1Nickname, String player2Nickname, String status) {
        this.matchId = matchId;
        this.player1Nickname = player1Nickname;
        this.player2Nickname = player2Nickname;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

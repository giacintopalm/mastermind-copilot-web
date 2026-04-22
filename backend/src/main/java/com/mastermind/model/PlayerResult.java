package com.mastermind.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "player_results", indexes = {
        @Index(name = "idx_player_nickname", columnList = "nickname")
})
public class PlayerResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private ResultType result;

    private Integer guessCount;

    private String opponent;

    private String matchId;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public ResultType getResult() { return result; }
    public void setResult(ResultType result) { this.result = result; }

    public Integer getGuessCount() { return guessCount; }
    public void setGuessCount(Integer guessCount) { this.guessCount = guessCount; }

    public String getOpponent() { return opponent; }
    public void setOpponent(String opponent) { this.opponent = opponent; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

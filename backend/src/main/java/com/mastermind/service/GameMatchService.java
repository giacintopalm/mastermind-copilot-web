package com.mastermind.service;

import com.mastermind.model.Game;
import com.mastermind.model.GameMatch;
import com.mastermind.model.PlayerResult;
import com.mastermind.model.ResultType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameMatchService {
    private final Map<String, GameMatch> matches = new ConcurrentHashMap<>();
    private final Map<String, String> nicknameToMatchId = new ConcurrentHashMap<>();
    private final GameService gameService;
    private final LeaderboardService leaderboardService;

    public GameMatchService(GameService gameService, LeaderboardService leaderboardService) {
        this.gameService = gameService;
        this.leaderboardService = leaderboardService;
    }

    /**
     * Create a new game match between two players
     */
    public GameMatch createMatch(String player1Nickname, String player2Nickname) {
        // Check if either player is already in a match
        if (nicknameToMatchId.containsKey(player1Nickname)) {
            throw new IllegalStateException(player1Nickname + " is already in a match");
        }
        if (nicknameToMatchId.containsKey(player2Nickname)) {
            throw new IllegalStateException(player2Nickname + " is already in a match");
        }

        GameMatch match = new GameMatch(player1Nickname, player2Nickname);
        matches.put(match.getMatchId(), match);
        nicknameToMatchId.put(player1Nickname, match.getMatchId());
        nicknameToMatchId.put(player2Nickname, match.getMatchId());

        return match;
    }

    /**
     * Set a player's game ID (when they submit their secret)
     */
    public GameMatch setPlayerGame(String nickname, String gameId) {
        String matchId = nicknameToMatchId.get(nickname);
        if (matchId == null) {
            throw new IllegalStateException("Player is not in a match");
        }

        GameMatch match = matches.get(matchId);
        if (match == null) {
            throw new IllegalStateException("Match not found");
        }

        if (match.getPlayer1Nickname().equals(nickname)) {
            match.setPlayer1GameId(gameId);
            match.setPlayer1Ready(true);
        } else if (match.getPlayer2Nickname().equals(nickname)) {
            match.setPlayer2GameId(gameId);
            match.setPlayer2Ready(true);
        } else {
            throw new IllegalStateException("Player not in this match");
        }

        // If both players are ready, update status
        if (match.areBothPlayersReady() && match.getStatus() == GameMatch.MatchStatus.SETUP) {
            match.setStatus(GameMatch.MatchStatus.PLAYING);
            match.setStartedAt(LocalDateTime.now());
        }

        return match;
    }

    /**
     * Get match by ID
     */
    public Optional<GameMatch> getMatch(String matchId) {
        return Optional.ofNullable(matches.get(matchId));
    }

    /**
     * Get match by player nickname
     */
    public Optional<GameMatch> getMatchByPlayer(String nickname) {
        String matchId = nicknameToMatchId.get(nickname);
        if (matchId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(matches.get(matchId));
    }

    /**
     * Check if a player is in a match
     */
    public boolean isPlayerInMatch(String nickname) {
        return nicknameToMatchId.containsKey(nickname);
    }

    /**
     * End a match
     */
    public void endMatch(String matchId) {
        GameMatch match = matches.remove(matchId);
        if (match != null) {
            // Try to record results for leaderboard before cleaning match
            try {
                String p1 = match.getPlayer1Nickname();
                String p2 = match.getPlayer2Nickname();

                Game g1 = match.getPlayer1GameId() == null ? null : gameService.getGame(match.getPlayer1GameId());
                Game g2 = match.getPlayer2GameId() == null ? null : gameService.getGame(match.getPlayer2GameId());

                boolean p1Won = g1 != null && g1.isWon();
                boolean p2Won = g2 != null && g2.isWon();
                boolean draw = p1Won && p2Won;

                Integer g1Guesses = g1 != null ? g1.getHistory().size() : null;
                Integer g2Guesses = g2 != null ? g2.getHistory().size() : null;

                PlayerResult r1 = new PlayerResult();
                r1.setNickname(p1);
                r1.setOpponent(p2);
                r1.setMatchId(match.getMatchId());
                r1.setGuessCount(g1Guesses);
                if (draw) r1.setResult(ResultType.DRAW);
                else r1.setResult(p1Won ? ResultType.WIN : ResultType.LOSS);
                leaderboardService.saveResult(r1);

                PlayerResult r2 = new PlayerResult();
                r2.setNickname(p2);
                r2.setOpponent(p1);
                r2.setMatchId(match.getMatchId());
                r2.setGuessCount(g2Guesses);
                if (draw) r2.setResult(ResultType.DRAW);
                else r2.setResult(p2Won ? ResultType.WIN : ResultType.LOSS);
                leaderboardService.saveResult(r2);
            } catch (Exception ex) {
                // Swallow to avoid preventing cleanup; log in future if logger available
            }

            nicknameToMatchId.remove(match.getPlayer1Nickname());
            nicknameToMatchId.remove(match.getPlayer2Nickname());
        }
    }

    /**
     * Cancel a match (remove player from match)
     */
    public void cancelMatch(String nickname) {
        String matchId = nicknameToMatchId.get(nickname);
        if (matchId != null) {
            GameMatch match = matches.get(matchId);
            if (match != null) {
                nicknameToMatchId.remove(match.getPlayer1Nickname());
                nicknameToMatchId.remove(match.getPlayer2Nickname());
                matches.remove(matchId);
            }
        }
    }

    /**
     * Get the game ID for a player's opponent
     */
    public String getOpponentGameId(String nickname) {
        Optional<GameMatch> matchOpt = getMatchByPlayer(nickname);
        if (matchOpt.isEmpty()) {
            return null;
        }

        GameMatch match = matchOpt.get();
        if (match.getPlayer1Nickname().equals(nickname)) {
            return match.getPlayer2GameId();
        } else {
            return match.getPlayer1GameId();
        }
    }
}

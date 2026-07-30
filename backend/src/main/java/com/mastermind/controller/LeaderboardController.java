package com.mastermind.controller;

import com.mastermind.dto.LeaderboardEntry;
import com.mastermind.model.PlayerResult;
import com.mastermind.model.ResultType;
import com.mastermind.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for leaderboard operations.
 * Exposes endpoints for recording game results and retrieving the top players.
 */
@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * Creates a {@code LeaderboardController} with the given service.
     *
     * @param leaderboardService the service used to persist and query results
     */
    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Records the result of a completed game.
     *
     * POST /leaderboard/result
     *
     * @param body a map containing {@code nickname} (String), {@code result} (WIN/LOSS/DRAW),
     *             {@code guessCount} (Integer, optional), {@code opponent} (String, optional),
     *             and {@code matchId} (String, optional)
     * @return {@code 200 OK} with {@code {"success": true}} on success, or
     *         {@code 400 Bad Request} with an error message on failure
     */
    @PostMapping("/result")
    public ResponseEntity<?> recordResult(@RequestBody Map<String, Object> body) {
        try {
            String nickname = (String) body.get("nickname");
            String result = (String) body.get("result");
            Integer guessCount = body.get("guessCount") == null ? null : ((Number) body.get("guessCount")).intValue();
            String opponent = (String) body.getOrDefault("opponent", null);
            String matchId = (String) body.getOrDefault("matchId", null);

            PlayerResult pr = new PlayerResult();
            pr.setNickname(nickname);
            pr.setResult(ResultType.valueOf(result));
            pr.setGuessCount(guessCount);
            pr.setOpponent(opponent);
            pr.setMatchId(matchId);

            leaderboardService.saveResult(pr);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    /**
     * Retrieves the top players ordered by wins and average guess count.
     *
     * GET /leaderboard/top?limit={limit}
     *
     * @param limit the maximum number of entries to return (default: 10)
     * @return {@code 200 OK} with the leaderboard entries
     */
    @GetMapping("/top")
    public ResponseEntity<List<LeaderboardEntry>> getTop(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getTopPlayers(limit));
    }
}

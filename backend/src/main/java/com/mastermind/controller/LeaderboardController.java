package com.mastermind.controller;

import com.mastermind.dto.LeaderboardEntry;
import com.mastermind.model.PlayerResult;
import com.mastermind.model.ResultType;
import com.mastermind.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

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

    @GetMapping("/top")
    public ResponseEntity<List<LeaderboardEntry>> getTop(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getTopPlayers(limit));
    }
}

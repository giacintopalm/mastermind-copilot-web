package com.mastermind.service;

import com.mastermind.dto.LeaderboardEntry;
import com.mastermind.model.PlayerResult;
import com.mastermind.repository.PlayerResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final PlayerResultRepository repo;

    public LeaderboardService(PlayerResultRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public PlayerResult saveResult(PlayerResult r) {
        return repo.save(r);
    }

    private LeaderboardEntry mapRow(Object[] row) {
        String nickname = (String) row[0];
        long wins = ((Number) row[1]).longValue();
        long games = ((Number) row[2]).longValue();
        double avgGuesses = row[3] == null ? 0.0 : ((Number) row[3]).doubleValue();
        Integer bestGuesses = row[4] == null ? null : ((Number) row[4]).intValue();
        return new LeaderboardEntry(nickname, wins, games, avgGuesses, bestGuesses);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopPlayers(int limit) {
        return repo.findLeaderboardRaw().stream()
            .map(this::mapRow)
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopMultiplayerPlayers(int limit) {
        return repo.findMultiplayerLeaderboardRaw().stream()
            .map(this::mapRow)
            .limit(limit)
            .collect(Collectors.toList());
    }
}

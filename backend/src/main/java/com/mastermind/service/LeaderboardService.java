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

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopPlayers(int limit) {
        List<Object[]> raw = repo.findLeaderboardRaw();
        return raw.stream()
            .map(row -> {
                String nickname = (String) row[0];
                long wins = ((Number) row[1]).longValue();
                long games = ((Number) row[2]).longValue();
                double avgGuesses = row[3] == null ? 0.0 : ((Number) row[3]).doubleValue();
                return new LeaderboardEntry(nickname, wins, games, avgGuesses);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }
}

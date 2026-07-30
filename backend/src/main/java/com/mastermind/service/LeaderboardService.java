package com.mastermind.service;

import com.mastermind.dto.LeaderboardEntry;
import com.mastermind.model.PlayerResult;
import com.mastermind.repository.PlayerResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for persisting and querying player results on the leaderboard.
 * Uses {@link PlayerResultRepository} for database access.
 */
@Service
public class LeaderboardService {

    private final PlayerResultRepository repo;

    /**
     * Creates a {@code LeaderboardService} with the given repository.
     *
     * @param repo the JPA repository used to persist and query player results
     */
    public LeaderboardService(PlayerResultRepository repo) {
        this.repo = repo;
    }

    /**
     * Persist a game result for a player.
     *
     * @param r the {@link PlayerResult} to save
     * @return the saved (and potentially auto-ID-assigned) entity
     */
    @Transactional
    public PlayerResult saveResult(PlayerResult r) {
        return repo.save(r);
    }

    /**
     * Retrieve the top-ranked players ordered by wins (descending) then average
     * guess count (ascending).
     *
     * @param limit the maximum number of leaderboard entries to return
     * @return list of {@link LeaderboardEntry} objects, at most {@code limit} elements
     */
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

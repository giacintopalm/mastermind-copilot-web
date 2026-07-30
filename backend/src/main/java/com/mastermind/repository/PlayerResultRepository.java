package com.mastermind.repository;

import com.mastermind.model.PlayerResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/**
 * Spring Data JPA repository for {@link com.mastermind.model.PlayerResult} entities.
 * Provides standard CRUD operations plus a custom JPQL query for leaderboard aggregation.
 */
public interface PlayerResultRepository extends JpaRepository<PlayerResult, Long> {

    /**
     * Aggregate player results into leaderboard rows.
     * Each row contains: nickname (String), wins (long), total games played (long),
     * and average guess count (Double, may be null if no games have a guess count).
     * Results are ordered by wins descending, then average guesses ascending.
     *
     * @return raw aggregate rows as {@code Object[]} arrays
     */
    @Query("""
      SELECT p.nickname as nickname,
             SUM(CASE WHEN p.result = 'WIN' THEN 1 ELSE 0 END) as wins,
             COUNT(p) as games,
             AVG(p.guessCount) as avgGuesses
      FROM PlayerResult p
      GROUP BY p.nickname
      ORDER BY wins DESC, avgGuesses ASC
    """)
    List<Object[]> findLeaderboardRaw();
}

package com.mastermind.repository;

import com.mastermind.model.PlayerResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PlayerResultRepository extends JpaRepository<PlayerResult, Long> {

    @Query("""
      SELECT p.nickname as nickname,
             SUM(CASE WHEN p.result = 'WIN' THEN 1 ELSE 0 END) as wins,
             COUNT(p) as games,
             AVG(p.guessCount) as avgGuesses,
             MIN(CASE WHEN p.result = 'WIN' THEN p.guessCount END) as bestGuesses
      FROM PlayerResult p
      GROUP BY p.nickname
      ORDER BY wins DESC, avgGuesses ASC
    """)
    List<Object[]> findLeaderboardRaw();

    @Query("""
      SELECT p.nickname as nickname,
             SUM(CASE WHEN p.result = 'WIN' THEN 1 ELSE 0 END) as wins,
             COUNT(p) as games,
             AVG(p.guessCount) as avgGuesses,
             MIN(CASE WHEN p.result = 'WIN' THEN p.guessCount END) as bestGuesses
      FROM PlayerResult p
      WHERE p.opponent IS NOT NULL
      GROUP BY p.nickname
      ORDER BY wins DESC, avgGuesses ASC
    """)
    List<Object[]> findMultiplayerLeaderboardRaw();
}

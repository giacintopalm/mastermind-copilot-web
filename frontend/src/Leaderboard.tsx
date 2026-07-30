import { useEffect, useState } from 'react'
import { leaderboardApi } from './api'

interface Props {
  mode?: 'all' | 'multiplayer'
  currentNickname?: string
}

export default function Leaderboard({ mode = 'all', currentNickname }: Props) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [entries, setEntries] = useState<Array<{ nickname: string; wins: number; games: number; avgGuesses: number; bestGuesses: number | null }>>([])

  useEffect(() => {
    load()
  }, [mode])

  async function load() {
    try {
      setLoading(true)
      setError(null)
      const data = mode === 'multiplayer'
        ? await leaderboardApi.getMultiplayerTop(20)
        : await leaderboardApi.getTop(20)
      setEntries(data)
    } catch (err) {
      setError('Failed to load leaderboard')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="leaderboard">
      <h2>{mode === 'multiplayer' ? 'Multiplayer Leaderboard' : 'Leaderboard'}</h2>
      {loading && <div>Loading...</div>}
      {error && <div className="error">{error}</div>}
      {!loading && !error && (
        <table className="leaderboard-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Player</th>
              <th>Wins</th>
              <th>Games</th>
              {mode === 'multiplayer' ? <th>Best Attempts</th> : <th>Avg Guesses</th>}
            </tr>
          </thead>
          <tbody>
            {entries.map((e, idx) => (
              <tr
                key={e.nickname}
                className={currentNickname && e.nickname === currentNickname ? 'leaderboard-highlight' : ''}
              >
                <td>{idx + 1}</td>
                <td>{e.nickname}{currentNickname && e.nickname === currentNickname ? ' ★' : ''}</td>
                <td>{e.wins}</td>
                <td>{e.games}</td>
                {mode === 'multiplayer'
                  ? <td>{e.bestGuesses != null ? e.bestGuesses : '-'}</td>
                  : <td>{e.avgGuesses ? e.avgGuesses.toFixed(2) : '-'}</td>
                }
              </tr>
            ))}
            {entries.length === 0 && (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center' }}>No results yet</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}

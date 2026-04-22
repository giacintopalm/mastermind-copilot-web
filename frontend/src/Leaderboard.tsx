import { useEffect, useState } from 'react'
import { leaderboardApi } from './api'

export default function Leaderboard() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [entries, setEntries] = useState<Array<{ nickname: string; wins: number; games: number; avgGuesses: number }>>([])

  useEffect(() => {
    load()
  }, [])

  async function load() {
    try {
      setLoading(true)
      setError(null)
      const data = await leaderboardApi.getTop(20)
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
      <h2>Leaderboard</h2>
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
              <th>Avg Guesses</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e, idx) => (
              <tr key={e.nickname}>
                <td>{idx + 1}</td>
                <td>{e.nickname}</td>
                <td>{e.wins}</td>
                <td>{e.games}</td>
                <td>{e.avgGuesses ? e.avgGuesses.toFixed(2) : '-'}</td>
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

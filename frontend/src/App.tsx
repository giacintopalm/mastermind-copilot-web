import { useEffect, useMemo, useState } from 'react'
import type { MouseEvent, DragEvent } from 'react'
import { gameApi, Color, GameState, GuessAttempt, ApiError } from './api'

const PALETTE: Color[] = ['red', 'blue', 'green', 'yellow', 'purple', 'cyan']
const SLOT_COUNT = 4

export default function App() {
  const [gameState, setGameState] = useState<GameState | null>(null)
  const [current, setCurrent] = useState<Color[]>(Array(SLOT_COUNT).fill(null as unknown as Color))
  const [selectedSlot, setSelectedSlot] = useState<number | null>(0)
  const [secret, setSecret] = useState<Color[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const win = useMemo(() => {
    if (!gameState || !gameState.history?.length) return false
    const last = gameState.history[gameState.history.length - 1]
    return last?.feedback?.exact === SLOT_COUNT
  }, [gameState])

  const gameOver = gameState?.gameOver ?? false

  // Initialize a new game on component mount
  useEffect(() => {
    createNewGame()
  }, [])

  async function createNewGame() {
    try {
      setLoading(true)
      setError(null)
      const newGame = await gameApi.createGame(SLOT_COUNT)
      setGameState(newGame)
      setCurrent(Array(SLOT_COUNT).fill(null as unknown as Color))
      setSelectedSlot(0)
      setSecret([]) // Reset secret for spoiler feature
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to create game'
      setError(message)
      console.error('Error creating game:', err)
    } finally {
      setLoading(false)
    }
  }

  async function loadGameSolution() {
    if (!gameState) return
    try {
      const solution = await gameApi.getGameSolution(gameState.id)
      setSecret(solution)
    } catch (err) {
      console.error('Error loading solution:', err)
    }
  }

  function chooseColor(c: Color) {
    if (gameOver || loading) return
    if (selectedSlot == null) return
    const next = [...current]
    next[selectedSlot] = c
    setCurrent(next)
    // advance to next empty slot
    const nextSlot = next.findIndex((x) => x == null)
    setSelectedSlot(nextSlot !== -1 ? nextSlot : null)
  }

  function clearSlot(i: number) {
    if (gameOver || loading) return
    const next = [...current]
    next[i] = null as unknown as Color
    setCurrent(next)
    setSelectedSlot(i)
  }

  async function submitGuess() {
    if (gameOver || loading || !gameState) return
    if (current.some((c: Color | null) => c == null)) return
    
    try {
      setLoading(true)
      setError(null)
      const updatedGame = await gameApi.submitGuess(gameState.id, current)
      setGameState(updatedGame)
      setCurrent(Array(SLOT_COUNT).fill(null as unknown as Color)) // Clear current guess
      setSelectedSlot(0) // Reset to first slot for next guess
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to submit guess'
      setError(message)
      console.error('Error submitting guess:', err)
    } finally {
      setLoading(false)
    }
  }

  async function getSuggestion() {
    if (gameOver || loading || !gameState) return
    
    try {
      setLoading(true)
      setError(null)
      const suggestion = await gameApi.getSuggestedGuess(gameState.id)
      
      if (suggestion === null) {
        setError('No suitable suggestion could be generated for this game state.')
        return
      }
      
      setCurrent(suggestion)
      setSelectedSlot(null) // Clear selection since all slots are filled
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to get suggestion'
      setError(message)
      console.error('Error getting suggestion:', err)
    } finally {
      setLoading(false)
    }
  }

  async function resetGame() {
    await createNewGame()
  }
  
  function handleDragStart(e: DragEvent<HTMLButtonElement>, color: Color | null, fromSlot?: number) {
    if (gameOver) return
    
    // Only allow dragging if there's a color
    if (!color) return
    
    // Set the data to transfer - color and optionally the slot index
    e.dataTransfer.setData('text/plain', JSON.stringify({
      color,
      fromSlot: fromSlot !== undefined ? fromSlot : null
    }))
    
    // Show a ghost image of the color when dragging
    e.dataTransfer.effectAllowed = 'move'
  }
  
  function handleDragOver(e: DragEvent<HTMLButtonElement>) {
    if (gameOver) return
    // Prevent default to allow drop
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }
  
  function handleDrop(e: DragEvent<HTMLButtonElement>, targetSlot: number) {
    if (gameOver) return
    e.preventDefault()
    
    try {
      // Get the transferred data
      const data = JSON.parse(e.dataTransfer.getData('text/plain'))
      const { color, fromSlot } = data
      
      if (!color) return
      
      const next = [...current]
      
      // If from another slot, swap colors
      if (fromSlot !== null && fromSlot !== targetSlot) {
        // Store target slot's color if any
        const targetColor = next[targetSlot]
        
        // Place dragged color in target slot
        next[targetSlot] = color
        
        // If target had a color, place it in the source slot
        if (targetColor) {
          next[fromSlot] = targetColor
        } else {
          // If target was empty, clear the source slot
          next[fromSlot] = null as unknown as Color
        }
      } else {
        // From palette to slot
        next[targetSlot] = color
      }
      
      setCurrent(next)
      setSelectedSlot(targetSlot)
    } catch (err) {
      console.error('Error processing drop:', err)
    }
  }

  return (
    <div className="app">
      <header>
        <h1>Mastermind</h1>
        <button className="secondary" onClick={resetGame}>New Game</button>
      </header>

      <main>
        <div className="main-game-layout">
          <section className="guess-section">
            <div className="section-header">
              <h2>Your Guess</h2>
            </div>
            <div className="section-content">
              <div className="game-layout">
                <div className="guess-container">
                  <div className="attempt-number current">?</div>
                  <div className="slots">
                  {Array.from({ length: SLOT_COUNT }).map((_, i) => {
                    const c = current[i]
                    return (
                      <button
                        key={i}
                        className={`slot ${c ?? ''} ${selectedSlot === i ? 'selected' : ''}`}
                        onClick={() => setSelectedSlot(i)}
                        draggable={c !== null}
                        onDragStart={(e) => handleDragStart(e, c, i)}
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, i)}
                        onContextMenu={(e: MouseEvent<HTMLButtonElement>) => {
                          e.preventDefault()
                          clearSlot(i)
                        }}
                        title={c ? `Slot ${i + 1}: ${c}` : `Slot ${i + 1}: empty (right-click to clear)`}
                      />
                    )
                  })}
                </div>
                
                <div className="actions">
                  <button
                    className="primary"
                    onClick={submitGuess}
                    disabled={gameOver || loading || current.some((c) => c == null)}
                  >
                    {loading ? 'Submitting...' : 'Submit'}
                  </button>
                  <button
                    className="primary"
                    onClick={getSuggestion}
                    disabled={gameOver || loading}
                  >
                    {loading ? 'Getting suggestion...' : 'Suggest'}
                  </button>
                </div>
                </div>
              </div>
            </div>
          </section>
          
          <div className="palette">
            <h3>Colors</h3>
            {PALETTE.map((c) => (
              <button
                key={c}
                className={`color ${c}`}
                onClick={() => chooseColor(c)}
                draggable
                onDragStart={(e) => handleDragStart(e, c)}
                title={`Pick ${c}`}
              />
            ))}
          </div>
        </div>
        
        {gameOver && (
          <div className="win-message">
            <div className="trophy">🏆</div>
            <div className="win-text">You cracked it!</div>
          </div>
        )}
        
        <section className="history-section">
          <div className="section-header">
            <h2>History</h2>
          </div>
          <div className="section-content">
            {!gameState?.history?.length ? (
              <p className="muted">No attempts yet. Pick colors and submit a guess.</p>
            ) : (
              <ul className="history">
                {gameState.history.map((h, idx) => (
                  <li key={idx} className="history-item">
                    <div className="attempt-number">{idx + 1}</div>
                    <div className="slots small">
                      {h.guess.map((c: Color, i: number) => (
                        <span key={i} className={`slot ${c.toLowerCase()}`} />
                      ))}
                    </div>
                    <div className="feedback">
                      {Array.from({ length: h.feedback.exact }).map((_, i) => (
                        <span key={"e"+i} className="peg exact" title="Exact match (black)" />
                      ))}
                      {Array.from({ length: h.feedback.partial }).map((_, i) => (
                        <span key={"p"+i} className="peg partial" title="Color-only match (white)" />
                      ))}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        <section className="solution-section">
          <div className="section-header">
            <h2>Solution</h2>
          </div>
          <div className="section-content">
            <details className="disclosure" onToggle={(e) => {
              if ((e.target as HTMLDetailsElement).open && secret.length === 0) {
                loadGameSolution()
              }
            }}>
              <summary>Show solution (spoiler)</summary>
              <div className="solution-item">
                <div className="attempt-number solution-number">✓</div>
                <div className="slots small solution-slots">
                  {secret.map((c, i) => (
                    <span key={i} className={`slot ${c.toLowerCase()}`} />
                  ))}
                </div>
                <div className="feedback solution-feedback">
                  <span className="solution-text">Secret Code</span>
                </div>
              </div>
            </details>
          </div>
        </section>

        {error && (
          <div className="error-message" style={{
            background: '#fee', 
            border: '1px solid #fcc', 
            borderRadius: '4px', 
            padding: '12px', 
            margin: '16px 0',
            color: '#c33'
          }}>
            <strong>Error:</strong> {error}
          </div>
        )}

        {loading && (
          <div className="loading-message" style={{
            background: '#eff', 
            border: '1px solid #cdf', 
            borderRadius: '4px', 
            padding: '12px', 
            margin: '16px 0',
            color: '#36c'
          }}>
            Loading...
          </div>
        )}
      </main>
    </div>
  )
}

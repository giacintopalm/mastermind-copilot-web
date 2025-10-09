import { useMemo, useState } from 'react'
import type { MouseEvent, DragEvent } from 'react'

type Color = 'red' | 'blue' | 'green' | 'yellow' | 'purple' | 'cyan'
const PALETTE: Color[] = ['red', 'blue', 'green', 'yellow', 'purple', 'cyan']

type Guess = {
  slots: Color[]
  feedback: { exact: number; partial: number }
}

const SLOT_COUNT = 4

function generateSecret(): Color[] {
  const secret: Color[] = []
  for (let i = 0; i < SLOT_COUNT; i++) {
    const idx = Math.floor(Math.random() * PALETTE.length)
    secret.push(PALETTE[idx])
  }
  return secret
}

function evaluateGuess(secret: Color[], guess: Color[]): { exact: number; partial: number } {
  // Exact matches first
  const secretCopy = [...secret]
  const guessCopy = [...guess]
  let exact = 0

  for (let i = 0; i < SLOT_COUNT; i++) {
    if (guessCopy[i] && guessCopy[i] === secretCopy[i]) {
      exact++
      // mark used
      secretCopy[i] = null as unknown as Color
      guessCopy[i] = null as unknown as Color
    }
  }

  // Partial matches (right color, wrong place)
  let partial = 0
  for (let i = 0; i < SLOT_COUNT; i++) {
    const g = guessCopy[i]
    if (!g) continue
    const idx = secretCopy.indexOf(g)
    if (idx !== -1) {
      partial++
      secretCopy[idx] = null as unknown as Color
      guessCopy[i] = null as unknown as Color
    }
  }

  return { exact, partial }
}

export default function App() {
  const [secret, setSecret] = useState<Color[]>(() => generateSecret())
  const [current, setCurrent] = useState<Color[]>(Array(SLOT_COUNT).fill(null as unknown as Color))
  const [selectedSlot, setSelectedSlot] = useState<number | null>(0)
  const [history, setHistory] = useState<Guess[]>([])
  const [gameOver, setGameOver] = useState(false)

  const win = useMemo(() => {
    const last = history.length ? history[history.length - 1] : undefined
    return last?.feedback.exact === SLOT_COUNT
  }, [history])

  function chooseColor(c: Color) {
    if (gameOver) return
    if (selectedSlot == null) return
    const next = [...current]
    next[selectedSlot] = c
    setCurrent(next)
    // advance to next empty slot
    const nextSlot = next.findIndex((x) => x == null)
    setSelectedSlot(nextSlot !== -1 ? nextSlot : null)
  }

  function clearSlot(i: number) {
    if (gameOver) return
    const next = [...current]
    next[i] = null as unknown as Color
    setCurrent(next)
    setSelectedSlot(i)
  }

  function submitGuess() {
    if (gameOver) return
  if (current.some((c: Color | null) => c == null)) return
    const feedback = evaluateGuess(secret, current)
    const guess: Guess = { slots: [...current], feedback }
    const nextHistory = [...history, guess]
    setHistory(nextHistory)
    // Keep the current guess instead of emptying it
    // setCurrent(Array(SLOT_COUNT).fill(null as unknown as Color))
    setSelectedSlot(null) // Deselect current slot after submission
    if (feedback.exact === SLOT_COUNT) setGameOver(true)
  }

  function resetGame() {
    setSecret(generateSecret())
    setCurrent(Array(SLOT_COUNT).fill(null as unknown as Color))
    setSelectedSlot(0)
    setHistory([])
    setGameOver(false)
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
                  disabled={gameOver || current.some((c) => c == null)}
                >
                  Submit
                </button>
              </div>
            </div>
            
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
          </div>
          
          {gameOver && (
  <div className="win-message">
    <div className="trophy">🏆</div>
    <div className="win-text">You cracked it!</div>
  </div>
)}
        </section>

        <section className="history-section">
          <div className="section-header">
            <h2>History</h2>
          </div>
          <div className="section-content">
            {history.length === 0 ? (
              <p className="muted">No attempts yet. Pick colors and submit a guess.</p>
            ) : (
              <ul className="history">
                {history.map((h, idx) => (
                  <li key={idx} className="history-item">
                    <div className="attempt-number">{idx + 1}</div>
                    <div className="slots small">
                      {h.slots.map((c, i) => (
                        <span key={i} className={`slot ${c}`} />
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
            <details className="disclosure">
              <summary>Show solution (spoiler)</summary>
              <div className="solution-item">
                <div className="attempt-number solution-number">✓</div>
                <div className="slots small solution-slots">
                  {secret.map((c, i) => (
                    <span key={i} className={`slot ${c}`} />
                  ))}
                </div>
                <div className="feedback solution-feedback">
                  <span className="solution-text">Secret Code</span>
                </div>
              </div>
            </details>
          </div>
        </section>
      </main>
    </div>
  )
}

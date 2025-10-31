import { useEffect, useMemo, useState, useRef } from 'react'
import type { MouseEvent, DragEvent } from 'react'
import { gameApi, Color, GameState, GuessAttempt, ApiError, API_BASE_URL } from './api'
import SockJS from 'sockjs-client'
import { Client, IMessage } from '@stomp/stompjs'

const PALETTE: Color[] = ['red', 'blue', 'green', 'yellow', 'purple', 'cyan']
const SLOT_COUNT = 4

type GameMode = 'solo' | 'computer' | 'multiplayer'
type GamePhase = 'setup' | 'playing' | 'finished'
type Turn = 'user' | 'computer'

export default function App() {
  // Game mode and phase
  const [gameMode, setGameMode] = useState<GameMode>('solo')
  const [gamePhase, setGamePhase] = useState<GamePhase>('setup')
  const [currentTurn, setCurrentTurn] = useState<Turn>('user')
  
  // User game state
  const [gameState, setGameState] = useState<GameState | null>(null)
  const [current, setCurrent] = useState<Color[]>(Array(SLOT_COUNT).fill(null as unknown as Color))
  const [selectedSlot, setSelectedSlot] = useState<number | null>(0)
  const [secret, setSecret] = useState<Color[]>([])
  
  // Computer game state
  const [computerGameState, setComputerGameState] = useState<GameState | null>(null)
  const [computerSecret, setComputerSecret] = useState<Color[]>(Array(SLOT_COUNT).fill(null as unknown as Color))
  const [selectedComputerSecretSlot, setSelectedComputerSecretSlot] = useState<number | null>(0)
  const [computerThinking, setComputerThinking] = useState(false)
  const [computerGameSolution, setComputerGameSolution] = useState<Color[]>([])
  
  // Multiplayer state
  const [multiplayerSession, setMultiplayerSession] = useState<{sessionId: string, nickname: string} | null>(null)
  const [activePlayers, setActivePlayers] = useState<Array<{sessionId: string, nickname: string, status: string}>>([])
  const [showNicknamePrompt, setShowNicknamePrompt] = useState(false)
  const stompClientRef = useRef<Client | null>(null)
  
  // Invitation state
  const [incomingInvitation, setIncomingInvitation] = useState<{invitationId: string, fromNickname: string} | null>(null)
  const [sentInvitation, setSentInvitation] = useState<{invitationId: string, toNickname: string} | null>(null)
  
  // Common state
  const [loading, setLoading] = useState(false)
  const [suggestLoading, setSuggestLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const win = useMemo(() => {
    if (!gameState || !gameState.history?.length) return false
    const last = gameState.history[gameState.history.length - 1]
    return last?.feedback?.exact === SLOT_COUNT
  }, [gameState])

  const computerWin = useMemo(() => {
    if (!computerGameState || !computerGameState.history?.length) return false
    const last = computerGameState.history[computerGameState.history.length - 1]
    return last?.feedback?.exact === SLOT_COUNT
  }, [computerGameState])

  const gameOver = gameState?.gameOver ?? false
  const computerGameOver = computerGameState?.gameOver ?? false
  
  // Determine game winner in computer mode
  const gameWinner = useMemo(() => {
    if (gameMode !== 'computer' || gamePhase !== 'finished') return null
    
    const userAttempts = gameState?.history?.length ?? 0
    const computerAttempts = computerGameState?.history?.length ?? 0
    
    if (win && computerWin) {
      if (userAttempts < computerAttempts) return 'user'
      if (computerAttempts < userAttempts) return 'computer'
      return 'draw'
    }
    if (win) return 'user'
    if (computerWin) return 'computer'
    
    // If neither won but both games are over, it's a draw
    if (gameOver && computerGameOver) return 'draw'
    
    return null
  }, [gameMode, gamePhase, win, computerWin, gameState?.history?.length, computerGameState?.history?.length, gameOver, computerGameOver])

  // Initialize a new game on component mount
  useEffect(() => {
    if (gameMode === 'solo') {
      createNewGame()
    } else {
      resetComputerMode()
    }
  }, [gameMode])

  // Auto-trigger computer moves
  useEffect(() => {
    if (gameMode === 'computer' && 
        gamePhase === 'playing' && 
        currentTurn === 'computer' && 
        !computerThinking && 
        computerGameState && 
        !computerGameOver) {
      makeComputerGuess()
    }
  }, [gameMode, gamePhase, currentTurn, computerThinking, computerGameState, computerGameOver])

  // WebSocket connection for multiplayer
  useEffect(() => {
    if (!multiplayerSession) {
      return
    }

    // Create WebSocket connection
    const socket = new SockJS(`${API_BASE_URL}/ws`)
    const client = new Client({
      webSocketFactory: () => socket as any,
      onConnect: () => {
        console.log('WebSocket connected')
        
        // Subscribe to player list updates
        client.subscribe('/topic/players', (message: IMessage) => {
          try {
            const playerList = JSON.parse(message.body)
            setActivePlayers(playerList.players || [])
          } catch (err) {
            console.error('Failed to parse player list:', err)
          }
        })

        // Subscribe to invitations for this player
        client.subscribe(`/topic/invitations/${multiplayerSession.nickname}`, (message: IMessage) => {
          try {
            const invitation = JSON.parse(message.body)
            console.log('Received invitation:', invitation)
            
            if (invitation.status === 'PENDING' && invitation.toNickname === multiplayerSession.nickname) {
              // Incoming invitation
              setIncomingInvitation({
                invitationId: invitation.invitationId,
                fromNickname: invitation.fromNickname
              })
            } else if (invitation.status === 'ACCEPTED' && invitation.fromNickname === multiplayerSession.nickname) {
              // Your invitation was accepted
              alert(`${invitation.toNickname} accepted your invitation! Game starting soon...`)
              setSentInvitation(null)
            } else if (invitation.status === 'DECLINED' && invitation.fromNickname === multiplayerSession.nickname) {
              // Your invitation was declined
              alert(`${invitation.toNickname} declined your invitation.`)
              setSentInvitation(null)
            } else if (invitation.status === 'CANCELLED') {
              // Invitation was cancelled
              setIncomingInvitation(null)
            }
          } catch (err) {
            console.error('Failed to parse invitation:', err)
          }
        })

        // Request initial player list
        fetchPlayerList()
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected')
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      }
    })

    client.activate()
    stompClientRef.current = client

    // Cleanup on unmount or session change
    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate()
      }
    }
  }, [multiplayerSession])

  async function fetchPlayerList() {
    if (!multiplayerSession) return
    
    try {
      const response = await fetch(
        `${API_BASE_URL}/multiplayer/players?exclude=${multiplayerSession.sessionId}`
      )
      const data = await response.json()
      setActivePlayers(data.players || [])
    } catch (err) {
      console.error('Failed to fetch player list:', err)
    }
  }

  async function handleSendInvitation(toNickname: string) {
    if (!multiplayerSession) return
    
    try {
      const response = await fetch(
        `${API_BASE_URL}/multiplayer/invite?fromNickname=${multiplayerSession.nickname}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ toNickname })
        }
      )
      
      if (response.ok) {
        const data = await response.json()
        setSentInvitation({ invitationId: data.invitationId, toNickname })
        console.log('Invitation sent:', data)
      } else {
        const errorData = await response.json()
        alert(errorData.message || 'Failed to send invitation')
      }
    } catch (err) {
      console.error('Failed to send invitation:', err)
      alert('Failed to send invitation')
    }
  }

  async function handleAcceptInvitation() {
    if (!multiplayerSession || !incomingInvitation) return
    
    try {
      const response = await fetch(
        `${API_BASE_URL}/multiplayer/invitation/respond?nickname=${multiplayerSession.nickname}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ 
            invitationId: incomingInvitation.invitationId, 
            accept: true 
          })
        }
      )
      
      if (response.ok) {
        const data = await response.json()
        console.log('Invitation accepted:', data)
        setIncomingInvitation(null)
        alert(`Accepted invitation from ${incomingInvitation.fromNickname}! Game starting soon...`)
        // TODO: Start multiplayer game
      }
    } catch (err) {
      console.error('Failed to accept invitation:', err)
    }
  }

  async function handleDeclineInvitation() {
    if (!multiplayerSession || !incomingInvitation) return
    
    try {
      await fetch(
        `${API_BASE_URL}/multiplayer/invitation/respond?nickname=${multiplayerSession.nickname}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ 
            invitationId: incomingInvitation.invitationId, 
            accept: false 
          })
        }
      )
      
      setIncomingInvitation(null)
    } catch (err) {
      console.error('Failed to decline invitation:', err)
    }
  }

  async function handleCancelInvitation() {
    if (!sentInvitation) return
    
    try {
      await fetch(
        `${API_BASE_URL}/multiplayer/invitation/cancel?invitationId=${sentInvitation.invitationId}`,
        { method: 'POST' }
      )
      
      setSentInvitation(null)
    } catch (err) {
      console.error('Failed to cancel invitation:', err)
    }
  }

  async function createNewGame() {
    try {
      setLoading(true)
      setError(null)
      const newGame = await gameApi.createGame(SLOT_COUNT)
      setGameState(newGame)
      setCurrent(Array(SLOT_COUNT).fill(null as unknown as Color))
      setSelectedSlot(0)
      setSecret([]) // Reset secret for spoiler feature
      
      if (gameMode === 'solo') {
        setGamePhase('playing')
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to create game'
      setError(message)
      console.error('Error creating game:', err)
    } finally {
      setLoading(false)
    }
  }

  async function createComputerGame() {
    try {
      setLoading(true)
      setError(null)
      
      // Create game for computer with user's secret as the target
      const request = {
        slotCount: SLOT_COUNT,
        secret: computerSecret.map(c => c.toString())
      }
      
      const response = await fetch(`${API_BASE_URL}/games`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      })
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        const message = errorData.message || `HTTP ${response.status}: ${response.statusText}`
        throw new ApiError(message, response.status)
      }
      
      const computerGame = await response.json()
      setComputerGameState(computerGame)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to create computer game'
      setError(message)
      console.error('Error creating computer game:', err)
    } finally {
      setLoading(false)
    }
  }

  function resetComputerMode() {
    setGamePhase('setup')
    setCurrentTurn('user')
    setGameState(null)
    setComputerGameState(null)
    setCurrent(Array(SLOT_COUNT).fill(null as unknown as Color))
    setComputerSecret(Array(SLOT_COUNT).fill(null as unknown as Color))
    setSelectedSlot(0)
    setSelectedComputerSecretSlot(0)
    setSecret([])
    setComputerGameSolution([])
    setComputerThinking(false)
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

  async function loadComputerGameSolution() {
    if (!computerGameState) return
    try {
      const solution = await gameApi.getGameSolution(computerGameState.id)
      setComputerGameSolution(solution)
    } catch (err) {
      console.error('Error loading computer game solution:', err)
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

  function chooseComputerSecretColor(c: Color) {
    if (gamePhase !== 'setup' || loading) return
    if (selectedComputerSecretSlot == null) return
    const next = [...computerSecret]
    next[selectedComputerSecretSlot] = c
    setComputerSecret(next)
    // advance to next empty slot
    const nextSlot = next.findIndex((x) => x == null)
    setSelectedComputerSecretSlot(nextSlot !== -1 ? nextSlot : null)
  }

  function clearSlot(i: number) {
    if (gameOver || loading) return
    const next = [...current]
    next[i] = null as unknown as Color
    setCurrent(next)
    setSelectedSlot(i)
  }

  function clearComputerSecretSlot(i: number) {
    if (gamePhase !== 'setup' || loading) return
    const next = [...computerSecret]
    next[i] = null as unknown as Color
    setComputerSecret(next)
    setSelectedComputerSecretSlot(i)
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
      
      // In computer mode, check if user won and handle turn switching
      if (gameMode === 'computer' && gamePhase === 'playing') {
        const userWon = updatedGame.history.length > 0 && 
          updatedGame.history[updatedGame.history.length - 1].feedback.exact === SLOT_COUNT
        
        if (userWon) {
          // User won, but computer gets one more turn if it hasn't finished
          if (!computerGameOver && computerGameState) {
            setCurrentTurn('computer')
          } else {
            setGamePhase('finished')
          }
        } else if (updatedGame.gameOver) {
          // User failed, computer wins if it has guessed correctly, otherwise draw
          setGamePhase('finished')
        } else {
          // Continue game, switch to computer turn
          setCurrentTurn('computer')
        }
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to submit guess'
      setError(message)
      console.error('Error submitting guess:', err)
    } finally {
      setLoading(false)
    }
  }

  async function makeComputerGuess() {
    if (!computerGameState || computerGameOver || computerThinking) return
    
    try {
      setComputerThinking(true)
      setError(null)
      
      const suggestion = await gameApi.getSuggestedGuess(computerGameState.id)
      
      if (suggestion === null) {
        setError('Computer could not generate a guess.')
        return
      }
      
      // Submit the computer's guess
      const updatedComputerGame = await gameApi.submitGuess(computerGameState.id, suggestion)
      setComputerGameState(updatedComputerGame)
      
      // Check if computer won
      const computerWon = updatedComputerGame.history.length > 0 && 
        updatedComputerGame.history[updatedComputerGame.history.length - 1].feedback.exact === SLOT_COUNT
      
      if (computerWon || updatedComputerGame.gameOver) {
        setGamePhase('finished')
      } else {
        // Switch back to user turn
        setCurrentTurn('user')
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Computer failed to make guess'
      setError(message)
      console.error('Error making computer guess:', err)
    } finally {
      setComputerThinking(false)
    }
  }

  async function getSuggestion() {
    if (gameOver || loading || suggestLoading || !gameState) return
    
    try {
      setSuggestLoading(true)
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
      setSuggestLoading(false)
    }
  }

  async function startComputerMode() {
    if (computerSecret.some(c => c == null)) {
      setError('Please set a complete secret for the computer to guess.')
      return
    }
    
    try {
      setLoading(true)
      setError(null)
      
      // Create user's game (computer generates secret automatically)
      await createNewGame()
      
      // Create computer's game with user-provided secret
      await createComputerGame()
      
      setGamePhase('playing')
      setCurrentTurn('user')
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to start computer mode'
      setError(message)
      console.error('Error starting computer mode:', err)
    } finally {
      setLoading(false)
    }
  }

  async function resetGame() {
    if (gameMode === 'solo') {
      await createNewGame()
    } else {
      resetComputerMode()
    }
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

  function handleRemoveDrop(e: DragEvent<HTMLDivElement>) {
    if (gameOver) return
    e.preventDefault()
    e.stopPropagation()
    console.log('Drop on remove zone') // Debug log
    
    try {
      // Get the transferred data
      const data = JSON.parse(e.dataTransfer.getData('text/plain'))
      const { fromSlot } = data
      console.log('Drop data:', data) // Debug log
      
      // Only remove if dragged from a slot (not from palette)
      if (fromSlot !== null && fromSlot !== undefined) {
        console.log('Removing from slot:', fromSlot) // Debug log
        clearSlot(fromSlot)
      }
    } catch (err) {
      console.error('Error processing remove drop:', err)
    }
  }

  function handleRemoveDragOver(e: DragEvent<HTMLDivElement>) {
    e.preventDefault()
    e.stopPropagation()
    e.dataTransfer.dropEffect = 'move'
    console.log('Drag over remove zone') // Debug log
  }

  // Drag handlers for computer secret setting
  function handleComputerSecretDragOver(e: DragEvent<HTMLButtonElement>) {
    if (gamePhase !== 'setup') return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }
  
  function handleComputerSecretDrop(e: DragEvent<HTMLButtonElement>, targetSlot: number) {
    if (gamePhase !== 'setup') return
    e.preventDefault()
    
    try {
      const data = JSON.parse(e.dataTransfer.getData('text/plain'))
      const { color, fromSlot } = data
      
      if (!color) return
      
      const next = [...computerSecret]
      
      if (fromSlot !== null && fromSlot !== targetSlot) {
        const targetColor = next[targetSlot]
        next[targetSlot] = color
        if (targetColor) {
          next[fromSlot] = targetColor
        } else {
          next[fromSlot] = null as unknown as Color
        }
      } else {
        next[targetSlot] = color
      }
      
      setComputerSecret(next)
      setSelectedComputerSecretSlot(targetSlot)
    } catch (err) {
      console.error('Error processing computer secret drop:', err)
    }
  }

  return (
    <div className="app">
      <header>
        <h1>Mastermind</h1>
        <div className="header-controls">
          <div className="mode-selector">
            <button 
              className={gameMode === 'solo' ? 'primary' : 'secondary'} 
              onClick={() => setGameMode('solo')}
              disabled={loading}
            >
              Solo
            </button>
            <button 
              className={gameMode === 'computer' ? 'primary' : 'secondary'} 
              onClick={() => setGameMode('computer')}
              disabled={loading}
            >
              Vs Computer
            </button>
            <button 
              className={gameMode === 'multiplayer' ? 'primary' : 'secondary'} 
              onClick={() => setGameMode('multiplayer')}
              disabled={loading}
            >
              Vs Other Player
            </button>
          </div>
          <button className="secondary action-button" onClick={resetGame} disabled={loading}>
            New Game
          </button>
        </div>
      </header>

      <main>
        {gameMode === 'solo' ? renderSoloMode() : gameMode === 'computer' ? renderComputerMode() : renderMultiplayerMode()}
        
        {error && (
          <div className="error-message">
            <strong>Error:</strong> {error}
          </div>
        )}

        {loading && (
          <div className="loading-message">
            Loading...
          </div>
        )}
      </main>
    </div>
  )

  function renderMultiplayerMode() {
    const handleNicknameSubmit = async (nickname: string) => {
      try {
        setLoading(true)
        setError(null)
        
        const response = await fetch(`${API_BASE_URL}/multiplayer/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ nickname })
        })
        
        const data = await response.json()
        
        if (data.success) {
          setMultiplayerSession({ sessionId: data.sessionId, nickname: data.nickname })
          setShowNicknamePrompt(false)
        } else {
          setError(data.message || 'Failed to login')
        }
      } catch (err) {
        setError('Failed to connect to server')
      } finally {
        setLoading(false)
      }
    }

    const handleLeaveLobby = async () => {
      if (!multiplayerSession) return
      
      try {
        await fetch(`${API_BASE_URL}/multiplayer/logout?sessionId=${multiplayerSession.sessionId}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        })
      } catch (err) {
        console.error('Failed to logout:', err)
      } finally {
        setMultiplayerSession(null)
        setActivePlayers([])
        setGameMode('solo')
        setGamePhase('setup')
      }
    }

    if (!multiplayerSession) {
      return (
        <div className="multiplayer-container">
          <div className="nickname-prompt">
            <h2>Join Multiplayer Lobby</h2>
            <p>Enter your nickname to join the lobby and play against other players.</p>
            <form 
              className="nickname-form"
              onSubmit={(e) => {
                e.preventDefault()
                const formData = new FormData(e.currentTarget)
                const nickname = formData.get('nickname') as string
                if (nickname && nickname.trim().length >= 3) {
                  handleNicknameSubmit(nickname.trim())
                }
              }}
            >
              <input
                className="nickname-input"
                type="text"
                name="nickname"
                placeholder="Your nickname"
                minLength={3}
                maxLength={20}
                pattern="[a-zA-Z0-9_-]+"
                required
                disabled={loading}
              />
              <button type="submit" className="primary" disabled={loading}>
                {loading ? 'Joining...' : 'Join Lobby'}
              </button>
            </form>
            <p style={{fontSize: '14px', color: 'var(--muted)', marginTop: '12px'}}>
              Nickname must be 3-20 characters (letters, numbers, _ and - only)
            </p>
          </div>
        </div>
      )
    }

    return (
      <div className="multiplayer-container">
        <div className="multiplayer-lobby">
          <div className="lobby-header">
            <h2>Multiplayer Lobby</h2>
            <p>Welcome, <strong>{multiplayerSession.nickname}</strong>!</p>
          </div>
          
          <div className="players-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3>Other Players ({activePlayers.length})</h3>
              <button 
                className="secondary" 
                onClick={fetchPlayerList}
                disabled={loading}
                style={{ padding: '6px 12px', fontSize: '14px' }}
              >
                {loading ? 'Refreshing...' : '🔄 Refresh'}
              </button>
            </div>
            {activePlayers.length === 0 ? (
              <div className="empty-lobby">
                <p>No other players logged in at the moment.</p>
                <p style={{ fontSize: '14px', color: 'var(--muted)', marginTop: '8px' }}>
                  Click Refresh to check for new players.
                </p>
              </div>
            ) : (
              <ul className="players-list">
                {activePlayers.map(player => (
                  <li key={player.sessionId} className="player-item">
                    <span className="player-nickname">{player.nickname}</span>
                    <span className={`player-status ${player.status.toLowerCase()}`}>
                      {player.status}
                    </span>
                    {sentInvitation?.toNickname === player.nickname ? (
                      <button 
                        className="cancel-invite-btn"
                        onClick={handleCancelInvitation}
                        title="Cancel invitation"
                      >
                        ❌ Cancel
                      </button>
                    ) : (
                      <button 
                        className="invite-btn"
                        onClick={() => handleSendInvitation(player.nickname)}
                        disabled={player.status !== 'AVAILABLE' || !!sentInvitation}
                        title={player.status !== 'AVAILABLE' ? 'Player not available' : 'Send invitation'}
                      >
                        ⚔️ Invite
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
          
          <div className="lobby-actions">
            <button 
              className="secondary" 
              onClick={handleLeaveLobby}
            >
              Leave Lobby
            </button>
          </div>
        </div>
        
        {/* Incoming Invitation Modal */}
        {incomingInvitation && (
          <div className="modal-overlay" onClick={handleDeclineInvitation}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
              <h3>🎮 Game Invitation</h3>
              <p>
                <strong>{incomingInvitation.fromNickname}</strong> wants to play Mastermind with you!
              </p>
              <div className="modal-actions">
                <button 
                  className="primary" 
                  onClick={handleAcceptInvitation}
                >
                  ✓ Accept
                </button>
                <button 
                  className="secondary" 
                  onClick={handleDeclineInvitation}
                >
                  ✗ Decline
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

  function renderSoloMode() {
    return (
      <>
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
                  disabled={gameOver || loading || suggestLoading}
                >
                  {suggestLoading ? 'Getting suggestion...' : 'Suggest'}
                </button>
              </div>
              </div>
            </div>
          </div>
        </section>
        
        <div className="shared-palette">
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
              <ul className="history">
                <li className="history-item">
                  <div className="attempt-number solution-number">✓</div>
                  <div className="slots small">
                    {secret.map((c, i) => (
                      <span key={i} className={`slot ${c.toLowerCase()}`} />
                    ))}
                  </div>
                  <div className="feedback solution-feedback">
                    <span className="solution-text">Secret Code</span>
                  </div>
                </li>
              </ul>
            </details>
          </div>
        </section>
      </>
    )
  }

  function renderComputerMode() {
    if (gamePhase === 'setup') {
      return renderComputerSetup()
    }
    
    if (gamePhase === 'finished') {
      return renderGameFinished()
    }
    
    return renderComputerGame()
  }

  function renderComputerSetup() {
    return (
      <>
        <section className="setup-section">
          <div className="section-header">
            <h2>Set Computer's Target</h2>
          </div>
          <div className="section-content">
            <p className="setup-instruction">
              Choose a secret code for the computer to guess:
            </p>
            <div className="game-layout">
              <div className="guess-container">
                <div className="attempt-number current">🎯</div>
                <div className="slots">
                  {Array.from({ length: SLOT_COUNT }).map((_, i) => {
                    const c = computerSecret[i]
                    return (
                      <button
                        key={i}
                        className={`slot ${c ?? ''} ${selectedComputerSecretSlot === i ? 'selected' : ''}`}
                        onClick={() => setSelectedComputerSecretSlot(i)}
                        draggable={c !== null}
                        onDragStart={(e) => handleDragStart(e, c, i)}
                        onDragOver={handleComputerSecretDragOver}
                        onDrop={(e) => handleComputerSecretDrop(e, i)}
                        onContextMenu={(e: MouseEvent<HTMLButtonElement>) => {
                          e.preventDefault()
                          clearComputerSecretSlot(i)
                        }}
                        title={c ? `Slot ${i + 1}: ${c}` : `Slot ${i + 1}: empty (right-click to clear)`}
                      />
                    )
                  })}
                </div>
                
                <div className="actions">
                  <button
                    className="primary"
                    onClick={startComputerMode}
                    disabled={loading || computerSecret.some((c) => c == null)}
                  >
                    {loading ? 'Starting...' : 'Start Game'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>
        
        <div className="shared-palette">
          {PALETTE.map((c) => (
            <button
              key={c}
              className={`color ${c}`}
              onClick={() => chooseComputerSecretColor(c)}
              draggable
              onDragStart={(e) => handleDragStart(e, c)}
              title={`Pick ${c}`}
            />
          ))}
        </div>
      </>
    )
  }

  function renderGameFinished() {
    return (
      <div className="game-finished">
        <div className="winner-announcement">
          {gameWinner === 'user' && (
            <div className="win-message user-win">
              <div className="trophy">🏆</div>
              <div className="win-text">You Won!</div>
              <div className="win-details">
                You cracked the code in {gameState?.history?.length} attempts, 
                computer needed {computerGameState?.history?.length} attempts.
              </div>
            </div>
          )}
          {gameWinner === 'computer' && (
            <div className="win-message computer-win">
              <div className="trophy">🤖</div>
              <div className="win-text">Computer Won!</div>
              <div className="win-details">
                Computer cracked the code in {computerGameState?.history?.length} attempts, 
                you needed {gameState?.history?.length} attempts.
              </div>
            </div>
          )}
          {gameWinner === 'draw' && (
            <div className="win-message draw">
              <div className="trophy">🤝</div>
              <div className="win-text">It's a Draw!</div>
              <div className="win-details">
                Both you and the computer solved your puzzles in the same number of attempts!
              </div>
            </div>
          )}
        </div>
        
        <div className="dual-board-summary">
          {renderPlayerBoard('Your Game', gameState, current, selectedSlot, 
            submitGuess, getSuggestion, 'user', secret, loadGameSolution, suggestLoading)}
          {renderPlayerBoard('Computer Game', computerGameState, [], null, 
            () => {}, () => {}, 'computer', computerGameSolution, loadComputerGameSolution, false)}
        </div>
      </div>
    )
  }

  function renderComputerGame() {
    return (
      <div className="computer-game">
        <div className="turn-indicator">
          <h2>
            {currentTurn === 'user' ? "Your Turn" : "Computer's Turn"}
          </h2>
        </div>
        
        <div className="dual-board">
          {renderPlayerBoard('Your Game', gameState, current, selectedSlot, 
            submitGuess, getSuggestion, 'user', secret, loadGameSolution, suggestLoading)}
          {renderPlayerBoard('Computer Game', computerGameState, [], null, 
            makeComputerGuess, () => {}, 'computer', computerGameSolution, loadComputerGameSolution, false)}
        </div>
        
        <div className="shared-palette">
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
    )
  }

  function renderPlayerBoard(
    title: string, 
    gameState: GameState | null, 
    currentGuess: Color[], 
    selectedSlot: number | null,
    onSubmit: () => void,
    onSuggest: () => void,
    player: 'user' | 'computer',
    solution: Color[],
    loadSolution: () => void,
    suggestLoading: boolean
  ) {
    const isUserTurn = currentTurn === 'user' && player === 'user'
    const isComputerTurn = currentTurn === 'computer' && player === 'computer'
    const isActive = gamePhase === 'playing' && (isUserTurn || isComputerTurn)
    const playerGameOver = gameState?.gameOver ?? false
    const playerWin = gameState?.history?.length && 
      gameState.history[gameState.history.length - 1]?.feedback?.exact === SLOT_COUNT

    return (
      <section className={`player-board ${player}-board ${isActive ? 'active' : ''}`}>
        <div className="section-header">
          <h2>{title}</h2>
          {playerWin && <span className="board-status win">✓ Won!</span>}
          {playerGameOver && !playerWin && <span className="board-status lose">✗ Failed</span>}
        </div>
        <div className="section-content">
          {gamePhase === 'playing' && !playerGameOver && player === 'user' && (
            <div className="guess-container">
              <div className="attempt-number current">?</div>
              <div className="slots">
                {Array.from({ length: SLOT_COUNT }).map((_, i) => {
                  const c = currentGuess[i]
                  return (
                    <button
                      key={i}
                      className={`slot ${c ?? ''} ${selectedSlot === i ? 'selected' : ''}`}
                      onClick={() => player === 'user' ? setSelectedSlot(i) : undefined}
                      draggable={c !== null && player === 'user'}
                      onDragStart={(e) => player === 'user' ? handleDragStart(e, c, i) : undefined}
                      onDragOver={player === 'user' ? handleDragOver : undefined}
                      onDrop={(e) => player === 'user' ? handleDrop(e, i) : undefined}
                      onContextMenu={player === 'user' ? (e: MouseEvent<HTMLButtonElement>) => {
                        e.preventDefault()
                        clearSlot(i)
                      } : undefined}
                      title={c ? `Slot ${i + 1}: ${c}` : `Slot ${i + 1}: empty`}
                      disabled={!isActive}
                    />
                  )
                })}
              </div>
              
              <div className="actions">
                <button
                  className="primary"
                  onClick={onSubmit}
                  disabled={!isUserTurn || loading || currentGuess.some((c) => c == null)}
                >
                  {loading ? 'Submitting...' : 'Submit'}
                </button>
                <button
                  className="primary"
                  onClick={onSuggest}
                  disabled={!isUserTurn || loading || suggestLoading}
                >
                  {suggestLoading ? 'Getting suggestion...' : 'Suggest'}
                </button>
              </div>
            </div>
          )}
          
          {/* Computer spacer to align with user's guess container */}
          {gamePhase === 'playing' && !playerGameOver && player === 'computer' && (
            <div className="guess-container-spacer" style={{
              minHeight: 'clamp(48px, 15vw, 64px)', 
              marginBottom: '0'
            }}></div>
          )}

          {/* History */}
          {gameState?.history?.length ? (
            <div className="history">
              {gameState.history.map((h, idx) => (
                <div key={idx} className="history-item">
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
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">No attempts yet.</p>
          )}

          {/* Show Solution */}
          {gameState && (
            <div className="solution-section-inline">
              <details className="disclosure" onToggle={(e) => {
                if ((e.target as HTMLDetailsElement).open && solution.length === 0) {
                  loadSolution()
                }
              }}>
                <summary>Show solution (spoiler)</summary>
                <ul className="history">
                  <li className="history-item">
                    <div className="attempt-number solution-number">✓</div>
                    <div className="slots small">
                      {solution.map((c, i) => (
                        <span key={i} className={`slot ${c.toLowerCase()}`} />
                      ))}
                    </div>
                    <div className="feedback solution-feedback">
                      <span className="solution-text">Secret Code</span>
                    </div>
                  </li>
                </ul>
              </details>
            </div>
          )}
        </div>
      </section>
    )
  }
}

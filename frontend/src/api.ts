// API service for communicating with the Java backend
const API_BASE_URL = import.meta.env.DEV 
  ? 'http://localhost:8080/api'
  : 'https://app-backend-y7teeb42qtz4k.azurewebsites.net/api'

export { API_BASE_URL }

export type Color = 'red' | 'blue' | 'green' | 'yellow' | 'purple' | 'cyan'

export interface Feedback {
  exact: number
  partial: number
}

export interface GuessAttempt {
  guess: Color[]
  feedback: Feedback
}

export interface GameState {
  id: string
  history: GuessAttempt[]
  gameOver: boolean
  won: boolean
  createdAt: string
  slotCount: number
}

export interface CreateGameRequest {
  slotCount?: number
}

export interface GuessRequest {
  colors: string[]
}

class ApiError extends Error {
  constructor(message: string, public status?: number) {
    super(message)
    this.name = 'ApiError'
  }
}

class GameApiService {

  private async handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      const message = errorData.message || `HTTP ${response.status}: ${response.statusText}`
      throw new ApiError(message, response.status)
    }
    return response.json()
  }

  /**
   * Create a new game
   */
  async createGame(slotCount: number = 4): Promise<GameState> {
    const body: CreateGameRequest = { slotCount }
    
    const response = await fetch(`${API_BASE_URL}/games`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })

    return this.handleResponse<GameState>(response)
  }

  /**
   * Get current game state
   */
  async getGame(gameId: string): Promise<GameState> {
    const response = await fetch(`${API_BASE_URL}/games/${gameId}`)
    return this.handleResponse<GameState>(response)
  }

  /**
   * Submit a guess for the game
   */
  async submitGuess(gameId: string, colors: Color[]): Promise<GameState> {
    const body: GuessRequest = {
      colors: colors.map(c => c.toString())
    }

    const response = await fetch(`${API_BASE_URL}/games/${gameId}/guesses`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    })

    return this.handleResponse<GameState>(response)
  }

  /**
   * Get the solution (secret code) for a game
   */
  async getGameSolution(gameId: string): Promise<Color[]> {
    const response = await fetch(`${API_BASE_URL}/games/${gameId}/solution`)
    const solutionStrings = await this.handleResponse<string[]>(response)
    return solutionStrings.map(s => s as Color)
  }

  /**
   * Reset/restart a game with a new secret
   */
  async resetGame(gameId: string): Promise<GameState> {
    const response = await fetch(`${API_BASE_URL}/games/${gameId}/reset`, {
      method: 'POST',
    })
    return this.handleResponse<GameState>(response)
  }

  /**
   * Get all available colors
   */
  async getAvailableColors(): Promise<Color[]> {
    const response = await fetch(`${API_BASE_URL}/games/colors`)
    const colorStrings = await this.handleResponse<string[]>(response)
    return colorStrings.map(s => s as Color)
  }

  /**
   * Get a suggested guess for the current game state
   */
  async getSuggestedGuess(gameId: string): Promise<Color[] | null> {
    const response = await fetch(`${API_BASE_URL}/games/${gameId}/suggest`)
    
    if (response.status === 204) {
      // No suggestion available
      return null
    }
    
    const suggestionStrings = await this.handleResponse<string[]>(response)
    return suggestionStrings.map(s => s as Color)
  }
}

export const gameApi = new GameApiService()
export { ApiError }

// Multiplayer types
export interface MultiplayerSession {
  sessionId: string
  nickname: string
}

export interface PlayerInfo {
  nickname: string
  status: string
}

export interface InvitationResponse {
  invitationId: string
  fromNickname: string
  toNickname: string
  status: string
  message?: string
}

class MultiplayerApiService {
  
  private async handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      const message = errorData.message || `HTTP ${response.status}: ${response.statusText}`
      throw new ApiError(message, response.status)
    }
    return response.json()
  }

  /**
   * Login to multiplayer lobby
   */
  async login(nickname: string): Promise<{ success: boolean; sessionId?: string; message?: string }> {
    const response = await fetch(`${API_BASE_URL}/multiplayer/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nickname })
    })
    return this.handleResponse(response)
  }

  /**
   * Logout from multiplayer lobby
   */
  async logout(sessionId: string): Promise<void> {
    await fetch(`${API_BASE_URL}/multiplayer/logout?sessionId=${sessionId}`, {
      method: 'POST'
    })
  }

  /**
   * Get list of active players
   */
  async getPlayers(): Promise<{ players: PlayerInfo[] }> {
    const response = await fetch(`${API_BASE_URL}/multiplayer/players`)
    return this.handleResponse(response)
  }

  /**
   * Send invitation to another player
   */
  async sendInvitation(fromNickname: string, toNickname: string): Promise<InvitationResponse> {
    const response = await fetch(`${API_BASE_URL}/multiplayer/invite?fromNickname=${fromNickname}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ toNickname })
    })
    return this.handleResponse(response)
  }

  /**
   * Respond to an invitation (accept or decline)
   */
  async respondToInvitation(nickname: string, invitationId: string, accept: boolean): Promise<InvitationResponse> {
    const response = await fetch(`${API_BASE_URL}/multiplayer/invitation/respond?nickname=${nickname}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ invitationId, accept })
    })
    return this.handleResponse(response)
  }

  /**
   * Cancel an invitation
   */
  async cancelInvitation(invitationId: string): Promise<InvitationResponse> {
    const response = await fetch(`${API_BASE_URL}/multiplayer/invitation/cancel?invitationId=${invitationId}`, {
      method: 'POST'
    })
    return this.handleResponse(response)
  }
}

export const multiplayerApi = new MultiplayerApiService()
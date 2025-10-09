package com.mastermind.service;

import com.mastermind.model.Color;
import com.mastermind.model.Feedback;
import com.mastermind.model.Game;
import com.mastermind.model.GuessAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing Mastermind game sessions.
 * Handles game creation, state management, and guess processing.
 */
@Service
public class GameService {

    private final GameLogicService gameLogicService;
    private final Map<String, Game> activeGames;
    private final Map<String, Feedback> gameFeedbacks;
    
    @Value("${mastermind.game.slot-count:4}")
    private int defaultSlotCount;

    @Autowired
    public GameService(GameLogicService gameLogicService) {
        this.gameLogicService = gameLogicService;
        this.activeGames = new ConcurrentHashMap<>();
        this.gameFeedbacks = new ConcurrentHashMap<>();
    }

    /**
     * Create a new game with default settings.
     * 
     * @return A new Game instance with generated secret
     */
    public Game createGame() {
        return createGame(defaultSlotCount);
    }

    /**
     * Create a new game with specified slot count.
     * 
     * @param slotCount Number of slots in the secret code
     * @return A new Game instance with generated secret
     */
    public Game createGame(int slotCount) {
        List<Color> secret = gameLogicService.generateSecret(slotCount);
        Game game = new Game(secret, slotCount);
        activeGames.put(game.getId(), game);
        return game;
    }

    /**
     * Get an existing game by ID.
     * 
     * @param gameId The unique game identifier
     * @return The game instance, or null if not found
     */
    public Game getGame(String gameId) {
        return activeGames.get(gameId);
    }

    /**
     * Submit a guess for an existing game.
     * 
     * @param gameId The unique game identifier
     * @param guessColors The colors in the guess
     * @return The updated game state with new guess and feedback
     * @throws IllegalArgumentException if game not found or guess is invalid
     * @throws IllegalStateException if game is already over
     */
    public Game submitGuess(String gameId, List<Color> guessColors) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        if (game.isGameOver()) {
            throw new IllegalStateException("Game is already over");
        }

        if (!gameLogicService.isValidGuess(guessColors, game.getSlotCount())) {
            throw new IllegalArgumentException("Invalid guess: must contain " + 
                                             game.getSlotCount() + " valid colors");
        }

        // Evaluate the guess against the secret
        Feedback feedback = gameLogicService.evaluateGuess(game.getSecret(), guessColors);
        gameFeedbacks.put(gameId, feedback);
        
        // Create and add the guess attempt to game history
        GuessAttempt guessAttempt = new GuessAttempt(guessColors, feedback);
        game.addGuessAttempt(guessAttempt);

        return game;
    }



    /**
     * Get the solution (secret code) for a game.
     * This is used for the spoiler feature in the frontend.
     * 
     * @param gameId The unique game identifier
     * @return The secret code, or null if game not found
     */
    public List<Color> getGameSolution(String gameId) {
        Game game = activeGames.get(gameId);
        return game != null ? game.getSecret() : null;
    }

    /**
     * Reset/restart an existing game with a new secret.
     * 
     * @param gameId The unique game identifier
     * @return The reset game, or null if game not found
     */
    public Game resetGame(String gameId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            return null;
        }

        // Generate new secret and reset game state
        List<Color> newSecret = gameLogicService.generateSecret(game.getSlotCount());
        game.setSecret(newSecret);
        game.getHistory().clear();
        game.setGameOver(false);
        game.setWon(false);

        return game;
    }

    /**
     * Remove a game from active games (cleanup).
     * 
     * @param gameId The unique game identifier
     * @return true if game was removed, false if not found
     */
    public boolean deleteGame(String gameId) {
        return activeGames.remove(gameId) != null;
    }

    /**
     * Get the count of active games (for monitoring).
     * 
     * @return Number of active games
     */
    public int getActiveGameCount() {
        return activeGames.size();
    }

    /**
     * Get a suggested guess for the current game state.
     * 
     * @param gameId The unique game identifier
     * @return A suggested guess based on the game history
     * @throws IllegalArgumentException if the game doesn't exist
     */
    public List<Color> suggestGuess(String gameId) {
        Game game = activeGames.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }
        
        return gameLogicService.suggestGuess(game.getHistory(), game.getSlotCount());
    }

    /**
     * Get all available colors for the game.
     * 
     * @return List of available colors
     */
    public List<Color> getAvailableColors() {
        return gameLogicService.getAvailableColors();
    }
}
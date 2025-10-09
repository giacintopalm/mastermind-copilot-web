package com.mastermind.controller;

import com.mastermind.dto.CreateGameRequest;
import com.mastermind.dto.ErrorResponse;
import com.mastermind.dto.GameResponse;
import com.mastermind.dto.GuessRequest;
import com.mastermind.model.Color;
import com.mastermind.model.Game;
import com.mastermind.service.GameService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Mastermind game operations.
 * Provides endpoints for game creation, guess submission, and game state management.
 */
@RestController
@RequestMapping("/games")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "https://nice-sand-04c84f41e.1.azurestaticapps.net", "https://mmgame.hyacinthwings.co.uk"})
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Create a new Mastermind game.
     * 
     * POST /api/games
     * 
     * @param request Optional request body with game settings
     * @return The created game (without secret)
     */
    @PostMapping
    public ResponseEntity<?> createGame(@Valid @RequestBody(required = false) CreateGameRequest request) {
        try {
            logger.debug("Creating new game with request: {}", request);
            
            Game game;
            if (request != null && request.getSlotCount() != null) {
                game = gameService.createGame(request.getSlotCount());
            } else {
                game = gameService.createGame();
            }
            
            logger.info("Created new game with ID: {}", game.getId());
            return ResponseEntity.ok(GameResponse.fromGame(game));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for game creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating game", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to create game"));
        }
    }

    /**
     * Get current game state.
     * 
     * GET /api/games/{gameId}
     * 
     * @param gameId The unique game identifier
     * @return The current game state (without secret)
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGame(@PathVariable String gameId) {
        try {
            logger.debug("Getting game state for ID: {}", gameId);
            
            Game game = gameService.getGame(gameId);
            if (game == null) {
                logger.warn("Game not found: {}", gameId);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(GameResponse.fromGame(game));
            
        } catch (Exception e) {
            logger.error("Error getting game: {}", gameId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to get game"));
        }
    }

    /**
     * Submit a guess for the game.
     * 
     * POST /api/games/{gameId}/guesses
     * 
     * @param gameId The unique game identifier
     * @param request The guess request containing colors
     * @return Updated game state with feedback
     */
    @PostMapping("/{gameId}/guesses")
    public ResponseEntity<?> submitGuess(@PathVariable String gameId, 
                                       @Valid @RequestBody GuessRequest request) {
        try {
            logger.debug("Submitting guess for game {}: {}", gameId, request);
            
            // Convert string colors to Color enum
            List<Color> colors;
            try {
                colors = request.getColors().stream()
                        .map(Color::fromString)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid color in guess: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_COLOR", "Invalid color: " + e.getMessage()));
            }
            
            Game updatedGame = gameService.submitGuess(gameId, colors);
            logger.info("Guess submitted for game {}, game over: {}, won: {}", 
                       gameId, updatedGame.isGameOver(), updatedGame.isWon());
            
            return ResponseEntity.ok(GameResponse.fromGame(updatedGame));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid guess for game {}: {}", gameId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_GUESS", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Invalid state for guess in game {}: {}", gameId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_STATE", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error submitting guess for game: {}", gameId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to submit guess"));
        }
    }

    /**
     * Get the solution (secret code) for a game.
     * Used for the spoiler feature.
     * 
     * GET /api/games/{gameId}/solution
     * 
     * @param gameId The unique game identifier
     * @return The secret code
     */
    @GetMapping("/{gameId}/solution")
    public ResponseEntity<?> getGameSolution(@PathVariable String gameId) {
        try {
            logger.debug("Getting solution for game: {}", gameId);
            
            List<Color> solution = gameService.getGameSolution(gameId);
            if (solution == null) {
                logger.warn("Game not found for solution request: {}", gameId);
                return ResponseEntity.notFound().build();
            }
            
            // Convert to string format for frontend compatibility
            List<String> solutionStrings = solution.stream()
                    .map(Color::getValue)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(solutionStrings);
            
        } catch (Exception e) {
            logger.error("Error getting solution for game: {}", gameId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to get solution"));
        }
    }

    /**
     * Reset/restart a game with a new secret.
     * 
     * POST /api/games/{gameId}/reset
     * 
     * @param gameId The unique game identifier
     * @return The reset game state
     */
    @PostMapping("/{gameId}/reset")
    public ResponseEntity<?> resetGame(@PathVariable String gameId) {
        try {
            logger.debug("Resetting game: {}", gameId);
            
            Game resetGame = gameService.resetGame(gameId);
            if (resetGame == null) {
                logger.warn("Game not found for reset: {}", gameId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("Game reset: {}", gameId);
            return ResponseEntity.ok(GameResponse.fromGame(resetGame));
            
        } catch (Exception e) {
            logger.error("Error resetting game: {}", gameId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to reset game"));
        }
    }

    /**
     * Get a suggested guess for the current game state.
     * 
     * GET /api/games/{gameId}/suggest
     * 
     * @param gameId The unique game identifier
     * @return A suggested guess based on the game history
     */
    @GetMapping("/{gameId}/suggest")
    public ResponseEntity<?> suggestGuess(@PathVariable String gameId) {
        try {
            logger.debug("Getting suggestion for game: {}", gameId);
            
            List<Color> suggestion = gameService.suggestGuess(gameId);
            
            if (suggestion == null) {
                logger.info("No suggestion available for game: {}", gameId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            
            // Convert Color enum to string for JSON response
            List<String> suggestionStrings = suggestion.stream()
                    .map(Color::getValue)
                    .collect(Collectors.toList());
            
            logger.info("Generated suggestion for game {}: {}", gameId, suggestionStrings);
            return ResponseEntity.ok(suggestionStrings);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for suggestion on game {}: {}", gameId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting suggestion for game: {}", gameId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to generate suggestion"));
        }
    }

    /**
     * Get all available colors for the game.
     * 
     * GET /api/games/colors
     * 
     * @return List of available colors
     */
    @GetMapping("/colors")
    public ResponseEntity<?> getAvailableColors() {
        try {
            List<String> colors = gameService.getAvailableColors().stream()
                    .map(Color::getValue)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(colors);
            
        } catch (Exception e) {
            logger.error("Error getting available colors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "Failed to get colors"));
        }
    }
}
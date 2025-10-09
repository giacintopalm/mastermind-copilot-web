package com.mastermind.service;

import com.mastermind.model.Color;
import com.mastermind.model.Feedback;
import com.mastermind.model.Game;
import com.mastermind.model.GuessAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GameService.
 * Tests game management, state tracking, and business logic.
 */
class GameServiceTest {

    @Mock
    private GameLogicService gameLogicService;

    private GameService gameService;

    private static final List<Color> TEST_SECRET = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW);
    private static final List<Color> TEST_GUESS = Arrays.asList(Color.RED, Color.BLUE, Color.PURPLE, Color.CYAN);
    private static final Feedback TEST_FEEDBACK = new Feedback(2, 0);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gameService = new GameService(gameLogicService);
        
        // Set default slot count using reflection
        ReflectionTestUtils.setField(gameService, "defaultSlotCount", 4);
    }

    @Test
    @DisplayName("Create game with default settings should work")
    void testCreateGame_DefaultSettings() {
        // Arrange
        when(gameLogicService.generateSecret(4)).thenReturn(TEST_SECRET);

        // Act
        Game game = gameService.createGame();

        // Assert
        assertNotNull(game);
        assertNotNull(game.getId());
        assertEquals(4, game.getSlotCount());
        assertEquals(TEST_SECRET, game.getSecret());
        assertFalse(game.isGameOver());
        assertFalse(game.isWon());
        assertTrue(game.getHistory().isEmpty());
        
        verify(gameLogicService).generateSecret(4);
    }

    @Test
    @DisplayName("Create game with custom slot count should work")
    void testCreateGame_CustomSlotCount() {
        // Arrange
        List<Color> customSecret = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN);
        when(gameLogicService.generateSecret(3)).thenReturn(customSecret);

        // Act
        Game game = gameService.createGame(3);

        // Assert
        assertNotNull(game);
        assertEquals(3, game.getSlotCount());
        assertEquals(customSecret, game.getSecret());
        
        verify(gameLogicService).generateSecret(3);
    }

    @Test
    @DisplayName("Get game should return existing game")
    void testGetGame_ExistingGame() {
        // Arrange
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        Game createdGame = gameService.createGame();

        // Act
        Game retrievedGame = gameService.getGame(createdGame.getId());

        // Assert
        assertNotNull(retrievedGame);
        assertEquals(createdGame.getId(), retrievedGame.getId());
        assertEquals(createdGame, retrievedGame);
    }

    @Test
    @DisplayName("Get game should return null for non-existing game")
    void testGetGame_NonExistingGame() {
        // Act
        Game game = gameService.getGame("non-existent-id");

        // Assert
        assertNull(game);
    }

    @Test
    @DisplayName("Submit guess should work for valid game and guess")
    void testSubmitGuess_ValidInput() {
        // Arrange
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        when(gameLogicService.isValidGuess(TEST_GUESS, 4)).thenReturn(true);
        when(gameLogicService.evaluateGuess(TEST_SECRET, TEST_GUESS)).thenReturn(TEST_FEEDBACK);

        Game game = gameService.createGame();
        String gameId = game.getId();

        // Act
        Game updatedGame = gameService.submitGuess(gameId, TEST_GUESS);

        // Assert
        assertNotNull(updatedGame);
        assertEquals(1, updatedGame.getHistory().size());
        assertEquals(TEST_GUESS, updatedGame.getHistory().get(0).getGuess());
        assertEquals(TEST_FEEDBACK, updatedGame.getHistory().get(0).getFeedback());
        
        verify(gameLogicService).isValidGuess(TEST_GUESS, 4);
        verify(gameLogicService).evaluateGuess(TEST_SECRET, TEST_GUESS);
    }

    @Test
    @DisplayName("Submit guess should throw exception for non-existing game")
    void testSubmitGuess_NonExistingGame() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> gameService.submitGuess("non-existent-id", TEST_GUESS));
    }

    @Test
    @DisplayName("Submit guess should throw exception for game already over")
    void testSubmitGuess_GameAlreadyOver() {
        // Arrange
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        Game game = gameService.createGame();
        
        // Manually set game as over using reflection
        ReflectionTestUtils.setField(game, "gameOver", true);

        // Act & Assert
        assertThrows(IllegalStateException.class, 
                () -> gameService.submitGuess(game.getId(), TEST_GUESS));
    }

    @Test
    @DisplayName("Submit guess should throw exception for invalid guess")
    void testSubmitGuess_InvalidGuess() {
        // Arrange
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        when(gameLogicService.isValidGuess(TEST_GUESS, 4)).thenReturn(false);

        Game game = gameService.createGame();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> gameService.submitGuess(game.getId(), TEST_GUESS));
        
        verify(gameLogicService).isValidGuess(TEST_GUESS, 4);
        verify(gameLogicService, never()).evaluateGuess(anyList(), anyList());
    }

    @Test
    @DisplayName("Submit winning guess should mark game as won and over")
    void testSubmitGuess_WinningGuess() {
        // Arrange
        Feedback winningFeedback = new Feedback(4, 0);
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        when(gameLogicService.isValidGuess(TEST_SECRET, 4)).thenReturn(true);
        when(gameLogicService.evaluateGuess(TEST_SECRET, TEST_SECRET)).thenReturn(winningFeedback);

        Game game = gameService.createGame();

        // Act
        Game updatedGame = gameService.submitGuess(game.getId(), TEST_SECRET);

        // Assert
        assertTrue(updatedGame.isWon());
        assertTrue(updatedGame.isGameOver());
    }

    @Test
    @DisplayName("Get game solution should return secret for existing game")
    void testGetGameSolution_ExistingGame() {
        // Arrange
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        Game game = gameService.createGame();

        // Act
        List<Color> solution = gameService.getGameSolution(game.getId());

        // Assert
        assertEquals(TEST_SECRET, solution);
    }

    @Test
    @DisplayName("Get game solution should return null for non-existing game")
    void testGetGameSolution_NonExistingGame() {
        // Act
        List<Color> solution = gameService.getGameSolution("non-existent-id");

        // Assert
        assertNull(solution);
    }

    @Test
    @DisplayName("Reset game should generate new secret and clear history")
    void testResetGame_ExistingGame() {
        // Arrange
        List<Color> newSecret = Arrays.asList(Color.PURPLE, Color.CYAN, Color.RED, Color.BLUE);
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET).thenReturn(newSecret);
        when(gameLogicService.isValidGuess(anyList(), anyInt())).thenReturn(true);
        when(gameLogicService.evaluateGuess(anyList(), anyList())).thenReturn(TEST_FEEDBACK);

        Game game = gameService.createGame();
        gameService.submitGuess(game.getId(), TEST_GUESS); // Add some history

        // Act
        Game resetGame = gameService.resetGame(game.getId());

        // Assert
        assertNotNull(resetGame);
        assertEquals(newSecret, resetGame.getSecret());
        assertTrue(resetGame.getHistory().isEmpty());
        assertFalse(resetGame.isGameOver());
        assertFalse(resetGame.isWon());
        
        verify(gameLogicService, times(2)).generateSecret(4); // Once for create, once for reset
    }

    @Test
    @DisplayName("Reset game should return null for non-existing game")
    void testResetGame_NonExistingGame() {
        // Act
        Game resetGame = gameService.resetGame("non-existent-id");

        // Assert
        assertNull(resetGame);
    }

    @Test
    @DisplayName("Delete game should remove existing game")
    void testDeleteGame_ExistingGame() {
        // Arrange
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        Game game = gameService.createGame();
        String gameId = game.getId();

        // Act
        boolean deleted = gameService.deleteGame(gameId);

        // Assert
        assertTrue(deleted);
        assertNull(gameService.getGame(gameId));
    }

    @Test
    @DisplayName("Delete game should return false for non-existing game")
    void testDeleteGame_NonExistingGame() {
        // Act
        boolean deleted = gameService.deleteGame("non-existent-id");

        // Assert
        assertFalse(deleted);
    }

    @Test
    @DisplayName("Get active game count should return correct count")
    void testGetActiveGameCount() {
        // Arrange
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);

        assertEquals(0, gameService.getActiveGameCount());

        // Act - create games
        Game game1 = gameService.createGame();
        assertEquals(1, gameService.getActiveGameCount());

        Game game2 = gameService.createGame();
        assertEquals(2, gameService.getActiveGameCount());

        // Act - delete game
        gameService.deleteGame(game1.getId());
        assertEquals(1, gameService.getActiveGameCount());

        gameService.deleteGame(game2.getId());
        assertEquals(0, gameService.getActiveGameCount());
    }

    @Test
    @DisplayName("Get available colors should delegate to game logic service")
    void testGetAvailableColors() {
        // Arrange
        List<Color> expectedColors = Arrays.asList(Color.values());
        when(gameLogicService.getAvailableColors()).thenReturn(expectedColors);

        // Act
        List<Color> availableColors = gameService.getAvailableColors();

        // Assert
        assertEquals(expectedColors, availableColors);
        verify(gameLogicService).getAvailableColors();
    }

    @Test
    @DisplayName("Multiple guesses should accumulate in history")
    void testMultipleGuesses_AccumulateHistory() {
        // Arrange
        List<Color> guess1 = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW);
        List<Color> guess2 = Arrays.asList(Color.PURPLE, Color.CYAN, Color.RED, Color.BLUE);
        
        when(gameLogicService.generateSecret(anyInt())).thenReturn(TEST_SECRET);
        when(gameLogicService.isValidGuess(anyList(), anyInt())).thenReturn(true);
        when(gameLogicService.evaluateGuess(anyList(), anyList())).thenReturn(TEST_FEEDBACK);

        Game game = gameService.createGame();

        // Act
        gameService.submitGuess(game.getId(), guess1);
        Game updatedGame = gameService.submitGuess(game.getId(), guess2);

        // Assert
        assertEquals(2, updatedGame.getHistory().size());
        assertEquals(guess1, updatedGame.getHistory().get(0).getGuess());
        assertEquals(guess2, updatedGame.getHistory().get(1).getGuess());
    }
}
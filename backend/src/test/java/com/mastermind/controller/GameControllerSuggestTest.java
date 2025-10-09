package com.mastermind.controller;

import com.mastermind.model.Color;
import com.mastermind.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GameController suggestGuess endpoint.
 */
class GameControllerSuggestTest {

    @Mock
    private GameService gameService;

    private GameController gameController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gameController = new GameController(gameService);
    }

    @Test
    @DisplayName("Suggest guess should return suggested colors")
    void testSuggestGuess_Success() {
        // Arrange
        String gameId = "test-game-123";
        List<Color> suggestion = Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW);
        when(gameService.suggestGuess(gameId)).thenReturn(suggestion);

        // Act
        ResponseEntity<?> response = gameController.suggestGuess(gameId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        List<String> expectedColors = Arrays.asList("red", "blue", "green", "yellow");
        assertEquals(expectedColors, response.getBody());
        verify(gameService).suggestGuess(gameId);
    }

    @Test
    @DisplayName("Suggest guess should handle null suggestion")
    void testSuggestGuess_NullSuggestion() {
        // Arrange
        String gameId = "test-game-123";
        when(gameService.suggestGuess(gameId)).thenReturn(null);

        // Act
        ResponseEntity<?> response = gameController.suggestGuess(gameId);

        // Assert
        assertEquals(204, response.getStatusCode().value());
        // Response should be empty (No Content)
        verify(gameService).suggestGuess(gameId);
    }

    @Test
    @DisplayName("Suggest guess should handle game not found")
    void testSuggestGuess_GameNotFound() {
        // Arrange
        String gameId = "non-existent-game";
        when(gameService.suggestGuess(gameId)).thenThrow(new IllegalArgumentException("Game not found"));

        // Act
        ResponseEntity<?> response = gameController.suggestGuess(gameId);

        // Assert
        assertEquals(404, response.getStatusCode().value());
        verify(gameService).suggestGuess(gameId);
    }

    @Test
    @DisplayName("Suggest guess should handle service exception")
    void testSuggestGuess_ServiceException() {
        // Arrange
        String gameId = "test-game-123";
        when(gameService.suggestGuess(gameId)).thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<?> response = gameController.suggestGuess(gameId);

        // Assert
        assertEquals(500, response.getStatusCode().value());
        verify(gameService).suggestGuess(gameId);
    }
}
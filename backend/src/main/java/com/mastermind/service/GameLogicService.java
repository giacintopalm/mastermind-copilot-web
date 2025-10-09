package com.mastermind.service;

import com.mastermind.model.Color;
import com.mastermind.model.Feedback;
import com.mastermind.model.GuessAttempt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Service class containing the core game logic for Mastermind.
 * Handles secret generation, guess evaluation, and game rules.
 */
@Service
public class GameLogicService {

    private static final Color[] AVAILABLE_COLORS = Color.values();
    private static final Random RANDOM = new SecureRandom();

    /**
     * Generate a random secret code for the game.
     * Equivalent to the TypeScript generateSecret() function.
     * 
     * @param slotCount Number of slots in the secret code
     * @return List of randomly selected colors
     */
    public List<Color> generateSecret(int slotCount) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("Slot count must be positive");
        }

        List<Color> secret = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            int randomIndex = RANDOM.nextInt(AVAILABLE_COLORS.length);
            secret.add(AVAILABLE_COLORS[randomIndex]);
        }
        return secret;
    }

    /**
     * Evaluate a guess against the secret code and return feedback.
     * Equivalent to the TypeScript evaluateGuess() function.
     * 
     * This method implements the standard Mastermind scoring algorithm:
     * 1. Count exact matches (correct color in correct position)
     * 2. Count partial matches (correct color in wrong position)
     * 
     * @param secret The secret code to guess
     * @param guess The player's guess
     * @return Feedback containing exact and partial match counts
     * @throws IllegalArgumentException if inputs are invalid
     */
    public Feedback evaluateGuess(List<Color> secret, List<Color> guess) {
        if (secret == null || guess == null) {
            throw new IllegalArgumentException("Secret and guess cannot be null");
        }
        
        if (secret.size() != guess.size()) {
            throw new IllegalArgumentException("Secret and guess must have same length");
        }

        int slotCount = secret.size();
        
        // Create copies to avoid modifying original lists
        Color[] secretCopy = secret.toArray(new Color[0]);
        Color[] guessCopy = guess.toArray(new Color[0]);
        
        // Phase 1: Count exact matches and mark used positions
        int exactMatches = 0;
        for (int i = 0; i < slotCount; i++) {
            if (guessCopy[i] != null && guessCopy[i] == secretCopy[i]) {
                exactMatches++;
                // Mark positions as used (equivalent to setting to null in TypeScript)
                secretCopy[i] = null;
                guessCopy[i] = null;
            }
        }

        // Phase 2: Count partial matches (right color, wrong position)
        int partialMatches = 0;
        for (int i = 0; i < slotCount; i++) {
            Color guessColor = guessCopy[i];
            if (guessColor == null) continue; // Already matched exactly
            
            // Find this color in remaining secret positions
            for (int j = 0; j < slotCount; j++) {
                if (secretCopy[j] == guessColor) {
                    partialMatches++;
                    secretCopy[j] = null; // Mark as used
                    guessCopy[i] = null;  // Mark as used
                    break; // Only count each guess color once
                }
            }
        }

        return new Feedback(exactMatches, partialMatches);
    }

    /**
     * Check if a guess results in a winning condition.
     * 
     * @param feedback The feedback from evaluating a guess
     * @param slotCount The number of slots in the game
     * @return true if the guess wins the game (all exact matches)
     */
    public boolean isWinningGuess(Feedback feedback, int slotCount) {
        return feedback.getExact() == slotCount;
    }

    /**
     * Validate that a guess contains only valid colors and has correct length.
     * 
     * @param guess The guess to validate
     * @param expectedLength Expected number of colors in the guess
     * @return true if guess is valid
     */
    public boolean isValidGuess(List<Color> guess, int expectedLength) {
        if (guess == null || guess.size() != expectedLength) {
            return false;
        }
        
        // Check that all colors are non-null (equivalent to checking for null in TypeScript)
        return guess.stream().allMatch(color -> color != null);
    }

    /**
     * Get all available colors for the game.
     * 
     * @return List of all available colors
     */
    public List<Color> getAvailableColors() {
        return Arrays.asList(AVAILABLE_COLORS);
    }

    /**
     * Suggest a guess that is compatible with the provided guess-feedback history.
     * This algorithm tries all possible guesses and returns the first that, when used as
     * a potential secret, would produce the same feedback for each historical guess.
     *
     * @param guessAttempts List of previous guess attempts (guess + feedback pairs)
     * @param slotCount Number of slots in the guess
     * @return A compatible guess, or null if none found
     */
    public List<Color> suggestGuess(List<GuessAttempt> guessAttempts, int slotCount) {
        List<Color> colors = getAvailableColors();
        int colorCount = colors.size();

        // Generate all possible guesses (cartesian product)
        int[] indices = new int[slotCount];
        while (true) {
            List<Color> candidate = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                candidate.add(colors.get(indices[i]));
            }

            // Check if this candidate is compatible with all previous guess attempts
            boolean compatible = true;
            for (GuessAttempt attempt : guessAttempts) {
                // If candidate were the secret, would evaluating the historical guess
                // produce the same feedback that was actually received?
                Feedback expectedFeedback = attempt.getFeedback();
                Feedback actualFeedback = evaluateGuess(candidate, attempt.getGuess());
                
                if (expectedFeedback.getExact() != actualFeedback.getExact() || 
                    expectedFeedback.getPartial() != actualFeedback.getPartial()) {
                    compatible = false;
                    break;
                }
            }
            
            if (compatible) {
                return candidate;
            }

            // Increment indices for next guess
            int pos = slotCount - 1;
            while (pos >= 0) {
                indices[pos]++;
                if (indices[pos] < colorCount) {
                    break;
                }
                indices[pos] = 0;
                pos--;
            }
            if (pos < 0) {
                break; // All combinations tried
            }
        }
        return null;
    }
}
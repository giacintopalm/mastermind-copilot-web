package com.mastermind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Mastermind game backend.
 * 
 * This application provides REST API endpoints for the Mastermind game,
 * including game creation, guess submission, and game state management.
 */
@SpringBootApplication
public class MastermindApplication {

    public static void main(String[] args) {
        SpringApplication.run(MastermindApplication.class, args);
    }
}
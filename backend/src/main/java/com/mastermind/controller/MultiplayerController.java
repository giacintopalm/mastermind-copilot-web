package com.mastermind.controller;

import com.mastermind.dto.LoginRequest;
import com.mastermind.dto.LoginResponse;
import com.mastermind.dto.PlayerListResponse;
import com.mastermind.model.PlayerSession;
import com.mastermind.service.PlayerSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/multiplayer")
public class MultiplayerController {

    private final PlayerSessionService playerSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MultiplayerController(PlayerSessionService playerSessionService, 
                                 SimpMessagingTemplate messagingTemplate) {
        this.playerSessionService = playerSessionService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Login endpoint - Creates a new player session
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            PlayerSession session = playerSessionService.login(request.getNickname());
            
            // Broadcast updated player list to all connected clients
            broadcastPlayerList();
            
            return ResponseEntity.ok(LoginResponse.success(session.getSessionId(), session.getNickname()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(LoginResponse.failure(e.getMessage()));
        }
    }

    /**
     * Logout endpoint - Removes a player session
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String sessionId) {
        playerSessionService.logout(sessionId);
        
        // Broadcast updated player list to all connected clients
        broadcastPlayerList();
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get list of active players
     */
    @GetMapping("/players")
    public ResponseEntity<PlayerListResponse> getPlayers(@RequestParam(required = false) String exclude) {
        PlayerListResponse response = playerSessionService.getPlayerList(exclude);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if nickname is available
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isTaken = playerSessionService.isNicknameTaken(nickname);
        return ResponseEntity.ok(!isTaken);
    }

    /**
     * WebSocket message handler for requesting player list updates
     */
    @MessageMapping("/players/refresh")
    @SendTo("/topic/players")
    public PlayerListResponse refreshPlayerList() {
        return playerSessionService.getPlayerList(null);
    }

    /**
     * Broadcast player list to all connected clients via WebSocket
     */
    private void broadcastPlayerList() {
        PlayerListResponse playerList = playerSessionService.getPlayerList(null);
        messagingTemplate.convertAndSend("/topic/players", playerList);
    }
}

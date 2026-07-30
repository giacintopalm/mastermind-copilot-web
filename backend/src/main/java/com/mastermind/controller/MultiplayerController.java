package com.mastermind.controller;

import com.mastermind.dto.*;
import com.mastermind.model.Game;
import com.mastermind.model.GameMatch;
import com.mastermind.model.Invitation;
import com.mastermind.model.PlayerSession;
import com.mastermind.service.GameMatchService;
import com.mastermind.service.GameService;
import com.mastermind.service.InvitationService;
import com.mastermind.service.PlayerSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST and WebSocket controller for multiplayer game operations.
 * Handles player sessions, invitations, game match setup, and real-time
 * notifications via STOMP/WebSocket.
 */
@RestController
@RequestMapping("/multiplayer")
public class MultiplayerController {

    private final PlayerSessionService playerSessionService;
    private final InvitationService invitationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameMatchService gameMatchService;
    private final GameService gameService;

    @Autowired
    public MultiplayerController(PlayerSessionService playerSessionService,
                                 InvitationService invitationService,
                                 SimpMessagingTemplate messagingTemplate,
                                 GameMatchService gameMatchService,
                                 GameService gameService) {
        this.playerSessionService = playerSessionService;
        this.invitationService = invitationService;
        this.messagingTemplate = messagingTemplate;
        this.gameMatchService = gameMatchService;
        this.gameService = gameService;
    }

    /**
     * Login endpoint - Creates a new player session.
     *
     * @param request the login request containing the desired nickname
     * @return {@code 200 OK} with the session ID and nickname on success, or
     *         {@code 409 Conflict} if the nickname is already taken
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
     * Logout endpoint - Removes a player session.
     *
     * @param sessionId the session ID of the player to log out
     * @return {@code 200 OK} after removing the session and broadcasting the updated player list
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String sessionId) {
        playerSessionService.logout(sessionId);
        
        // Broadcast updated player list to all connected clients
        broadcastPlayerList();
        
        return ResponseEntity.ok().build();
    }

    /**
     * Get list of active players.
     *
     * @param exclude session ID of the requesting player to exclude from the list (optional)
     * @return {@code 200 OK} with the list of currently connected players
     */
    @GetMapping("/players")
    public ResponseEntity<PlayerListResponse> getPlayers(@RequestParam(required = false) String exclude) {
        // Update activity for the requesting player
        if (exclude != null) {
            playerSessionService.updatePlayerActivity(exclude);
        }
        PlayerListResponse response = playerSessionService.getPlayerList(exclude);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if nickname is available.
     *
     * @param nickname the nickname to check
     * @return {@code 200 OK} with {@code true} if available, {@code false} if already taken
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isTaken = playerSessionService.isNicknameTaken(nickname);
        return ResponseEntity.ok(!isTaken);
    }

    /**
     * WebSocket message handler for requesting player list updates.
     * Clients send a message to {@code /app/players/refresh}; the updated list
     * is broadcast to all subscribers of {@code /topic/players}.
     *
     * @return the current list of connected players
     */
    @MessageMapping("/players/refresh")
    @SendTo("/topic/players")
    public PlayerListResponse refreshPlayerList() {
        return playerSessionService.getPlayerList(null);
    }

    /**
     * Broadcast player list to all connected clients via WebSocket.
     * Sends the updated list to the {@code /topic/players} STOMP destination.
     */
    private void broadcastPlayerList() {
        PlayerListResponse playerList = playerSessionService.getPlayerList(null);
        messagingTemplate.convertAndSend("/topic/players", playerList);
    }

    /**
     * Send invitation to another player.
     *
     * @param fromNickname the nickname of the player sending the invitation
     * @param request      the invitation request containing the recipient's nickname
     * @return {@code 200 OK} with the created invitation, or
     *         {@code 400 Bad Request} if the invitation cannot be created
     */
    @PostMapping("/invite")
    public ResponseEntity<?> sendInvitation(@RequestParam String fromNickname,
                                            @Valid @RequestBody InvitationRequest request) {
        try {
            // Update sender's activity
            playerSessionService.updatePlayerActivityByNickname(fromNickname);
            
            Invitation invitation = invitationService.createInvitation(fromNickname, request.getToNickname());
            
            InvitationResponse response = new InvitationResponse(
                    invitation.getInvitationId(),
                    invitation.getFromNickname(),
                    invitation.getToNickname(),
                    invitation.getStatus().toString()
            );
            response.setMessage("Invitation sent successfully");
            
            // Send invitation to the recipient via WebSocket
            messagingTemplate.convertAndSend("/topic/invitations/" + request.getToNickname(), response);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVITATION_ERROR", e.getMessage()));
        }
    }

    /**
     * Respond to an invitation (accept or decline).
     *
     * @param nickname the nickname of the player responding
     * @param request  the action request indicating acceptance or declination and the invitation ID
     * @return {@code 200 OK} with the updated invitation, or
     *         {@code 400 Bad Request} if the invitation cannot be found or is no longer pending
     */
    @PostMapping("/invitation/respond")
    public ResponseEntity<?> respondToInvitation(@RequestParam String nickname,
                                                  @Valid @RequestBody InvitationActionRequest request) {
        try {
            Invitation invitation;
            if (request.isAccept()) {
                invitation = invitationService.acceptInvitation(request.getInvitationId());
            } else {
                invitation = invitationService.declineInvitation(request.getInvitationId());
            }
            
            InvitationResponse response = new InvitationResponse(
                    invitation.getInvitationId(),
                    invitation.getFromNickname(),
                    invitation.getToNickname(),
                    invitation.getStatus().toString()
            );
            
            if (request.isAccept()) {
                response.setMessage("Invitation accepted! Starting game...");
                // Notify the inviter that their invitation was accepted
                messagingTemplate.convertAndSend("/topic/invitations/" + invitation.getFromNickname(), response);
            } else {
                response.setMessage("Invitation declined");
                // Notify the inviter that their invitation was declined
                messagingTemplate.convertAndSend("/topic/invitations/" + invitation.getFromNickname(), response);
            }
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVITATION_ERROR", e.getMessage()));
        }
    }

    /**
     * Cancel an invitation.
     *
     * @param invitationId the ID of the invitation to cancel
     * @return {@code 200 OK} with the cancelled invitation, {@code 404 Not Found} if the
     *         invitation does not exist, or {@code 400 Bad Request} on other errors
     */
    @PostMapping("/invitation/cancel")
    public ResponseEntity<?> cancelInvitation(@RequestParam String invitationId) {
        try {
            Invitation invitation = invitationService.getInvitation(invitationId);
            if (invitation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("NOT_FOUND", "Invitation not found"));
            }
            
            invitationService.cancelInvitationsForPlayer(invitation.getFromNickname());
            
            // Notify the recipient that the invitation was cancelled
            InvitationResponse response = new InvitationResponse(
                    invitation.getInvitationId(),
                    invitation.getFromNickname(),
                    invitation.getToNickname(),
                    "CANCELLED"
            );
            response.setMessage("Invitation cancelled");
            messagingTemplate.convertAndSend("/topic/invitations/" + invitation.getToNickname(), response);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVITATION_ERROR", e.getMessage()));
        }
    }

    /**
     * Set player's secret and create a game when an invitation is accepted.
     * When both players have submitted their secrets the match transitions to
     * the PLAYING state and both are notified via WebSocket.
     *
     * @param nickname         the nickname of the player setting their secret
     * @param opponentNickname the nickname of the opponent
     * @param request          the request body containing the player's chosen secret colors
     * @return {@code 200 OK} with the current match state, or
     *         {@code 400 Bad Request} on error
     */
    @PostMapping("/game/set-secret")
    public ResponseEntity<?> setSecret(@RequestParam String nickname,
                                        @RequestParam String opponentNickname,
                                        @Valid @RequestBody SetSecretRequest request) {
        try {
            // Create or get existing match
            GameMatch match;
            if (!gameMatchService.isPlayerInMatch(nickname)) {
                // Create match when first player sets secret
                match = gameMatchService.createMatch(nickname, opponentNickname);
            } else {
                match = gameMatchService.getMatchByPlayer(nickname)
                        .orElseThrow(() -> new IllegalStateException("Match not found"));
            }

            // Create a game with the player's secret as the target
            // Convert string colors to Color enum
            List<com.mastermind.model.Color> secretColors = request.getSecret().stream()
                    .map(s -> com.mastermind.model.Color.valueOf(s.toUpperCase()))
                    .toList();
            
            Game game = gameService.createGameWithSecret(4, secretColors);
            
            // Set the game ID for this player
            match = gameMatchService.setPlayerGame(nickname, game.getId());

            // Create response
            GameMatchResponse response = new GameMatchResponse(
                    match.getMatchId(),
                    match.getPlayer1Nickname(),
                    match.getPlayer2Nickname(),
                    match.getStatus().toString()
            );
            response.setPlayer1GameId(match.getPlayer1GameId());
            response.setPlayer2GameId(match.getPlayer2GameId());
            response.setPlayer1Ready(match.isPlayer1Ready());
            response.setPlayer2Ready(match.isPlayer2Ready());

            // If both players are ready, notify them to start the game
            if (match.areBothPlayersReady()) {
                response.setMessage("Both players ready! Game starting...");
                
                // Mark both players as IN_GAME
                playerSessionService.updatePlayerActivityByNickname(match.getPlayer1Nickname());
                playerSessionService.getSessionByNickname(match.getPlayer1Nickname())
                        .ifPresent(s -> playerSessionService.updatePlayerStatus(s.getSessionId(), PlayerSession.PlayerStatus.IN_GAME));
                
                playerSessionService.updatePlayerActivityByNickname(match.getPlayer2Nickname());
                playerSessionService.getSessionByNickname(match.getPlayer2Nickname())
                        .ifPresent(s -> playerSessionService.updatePlayerStatus(s.getSessionId(), PlayerSession.PlayerStatus.IN_GAME));
                
                // Broadcast updated player list (showing players as busy)
                broadcastPlayerList();
                
                // Notify both players via WebSocket
                messagingTemplate.convertAndSend("/topic/game/" + match.getPlayer1Nickname(), response);
                messagingTemplate.convertAndSend("/topic/game/" + match.getPlayer2Nickname(), response);
            } else {
                response.setMessage("Waiting for opponent to set their secret...");
                // Update activity for the player who just set their secret
                playerSessionService.updatePlayerActivityByNickname(nickname);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("GAME_ERROR", e.getMessage()));
        }
    }

    /**
     * Get current match status for a player.
     *
     * @param nickname the nickname of the player whose match status is requested
     * @return {@code 200 OK} with the match state, or {@code 400 Bad Request} if
     *         the player is not currently in a match
     */
    @GetMapping("/game/status")
    public ResponseEntity<?> getGameStatus(@RequestParam String nickname) {
        try {
            GameMatch match = gameMatchService.getMatchByPlayer(nickname)
                    .orElseThrow(() -> new IllegalStateException("Player is not in a match"));

            GameMatchResponse response = new GameMatchResponse(
                    match.getMatchId(),
                    match.getPlayer1Nickname(),
                    match.getPlayer2Nickname(),
                    match.getStatus().toString()
            );
            response.setPlayer1GameId(match.getPlayer1GameId());
            response.setPlayer2GameId(match.getPlayer2GameId());
            response.setPlayer1Ready(match.isPlayer1Ready());
            response.setPlayer2Ready(match.isPlayer2Ready());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("GAME_ERROR", e.getMessage()));
        }
    }

    /**
     * Mark player as available (returned to lobby).
     * Updates the player's status to AVAILABLE and broadcasts the updated
     * player list to all connected clients.
     *
     * @param nickname the nickname of the player returning to the lobby
     * @return {@code 200 OK}
     */
    @PostMapping("/player/available")
    public ResponseEntity<Void> markPlayerAvailable(@RequestParam String nickname) {
        playerSessionService.updatePlayerActivityByNickname(nickname);
        playerSessionService.getSessionByNickname(nickname)
                .ifPresent(s -> playerSessionService.updatePlayerStatus(s.getSessionId(), PlayerSession.PlayerStatus.AVAILABLE));
        
        // Broadcast updated player list
        broadcastPlayerList();
        
        return ResponseEntity.ok().build();
    }
}

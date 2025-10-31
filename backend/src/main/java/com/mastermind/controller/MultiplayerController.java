package com.mastermind.controller;

import com.mastermind.dto.*;
import com.mastermind.model.Invitation;
import com.mastermind.model.PlayerSession;
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

@RestController
@RequestMapping("/multiplayer")
public class MultiplayerController {

    private final PlayerSessionService playerSessionService;
    private final InvitationService invitationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MultiplayerController(PlayerSessionService playerSessionService,
                                 InvitationService invitationService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.playerSessionService = playerSessionService;
        this.invitationService = invitationService;
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

    /**
     * Send invitation to another player
     */
    @PostMapping("/invite")
    public ResponseEntity<?> sendInvitation(@RequestParam String fromNickname,
                                            @Valid @RequestBody InvitationRequest request) {
        try {
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
     * Respond to an invitation (accept or decline)
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
     * Cancel an invitation
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
}

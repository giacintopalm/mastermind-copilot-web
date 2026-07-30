package com.mastermind.service;

import com.mastermind.model.Invitation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing player-to-player game invitations.
 * Invitations are held in memory; expired or resolved invitations can be cleaned up
 * via {@link #cleanupExpiredInvitations()}.
 */
@Service
public class InvitationService {
    private final Map<String, Invitation> invitations = new ConcurrentHashMap<>();
    private final PlayerSessionService playerSessionService;

    /**
     * Creates an {@code InvitationService} with the given session service.
     *
     * @param playerSessionService the service used to validate that players are connected
     */
    public InvitationService(PlayerSessionService playerSessionService) {
        this.playerSessionService = playerSessionService;
    }

    /**
     * Create a new invitation from one player to another.
     *
     * @param fromNickname the nickname of the player sending the invitation
     * @param toNickname   the nickname of the player receiving the invitation
     * @return the newly created {@link Invitation}
     * @throws IllegalStateException if either player is not currently connected, or if a
     *                               pending invitation already exists between the two players
     */
    public Invitation createInvitation(String fromNickname, String toNickname) {
        // Validate both players exist and are available
        if (!playerSessionService.isNicknameConnected(fromNickname)) {
            throw new IllegalStateException("Sender not in lobby");
        }
        if (!playerSessionService.isNicknameConnected(toNickname)) {
            throw new IllegalStateException("Recipient not in lobby");
        }

        // Check if there's already a pending invitation between these players
        boolean hasPendingInvitation = invitations.values().stream()
                .anyMatch(inv -> inv.getStatus() == Invitation.InvitationStatus.PENDING &&
                        ((inv.getFromNickname().equals(fromNickname) && inv.getToNickname().equals(toNickname)) ||
                                (inv.getFromNickname().equals(toNickname) && inv.getToNickname().equals(fromNickname))));

        if (hasPendingInvitation) {
            throw new IllegalStateException("There is already a pending invitation between these players");
        }

        Invitation invitation = new Invitation(fromNickname, toNickname);
        invitations.put(invitation.getInvitationId(), invitation);
        return invitation;
    }

    /**
     * Accept a pending invitation.
     *
     * @param invitationId the unique identifier of the invitation to accept
     * @return the updated {@link Invitation} with status ACCEPTED
     * @throws IllegalArgumentException if the invitation does not exist
     * @throws IllegalStateException    if the invitation is no longer pending
     */
    public Invitation acceptInvitation(String invitationId) {
        Invitation invitation = invitations.get(invitationId);
        if (invitation == null) {
            throw new IllegalArgumentException("Invitation not found");
        }
        if (invitation.getStatus() != Invitation.InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is no longer pending");
        }

        invitation.setStatus(Invitation.InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        return invitation;
    }

    /**
     * Decline a pending invitation.
     *
     * @param invitationId the unique identifier of the invitation to decline
     * @return the updated {@link Invitation} with status DECLINED
     * @throws IllegalArgumentException if the invitation does not exist
     * @throws IllegalStateException    if the invitation is no longer pending
     */
    public Invitation declineInvitation(String invitationId) {
        Invitation invitation = invitations.get(invitationId);
        if (invitation == null) {
            throw new IllegalArgumentException("Invitation not found");
        }
        if (invitation.getStatus() != Invitation.InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is no longer pending");
        }

        invitation.setStatus(Invitation.InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        return invitation;
    }

    /**
     * Retrieve an invitation by its ID.
     *
     * @param invitationId the unique identifier of the invitation
     * @return the {@link Invitation}, or {@code null} if not found
     */
    public Invitation getInvitation(String invitationId) {
        return invitations.get(invitationId);
    }

    /**
     * Get all pending invitations addressed to a specific player.
     *
     * @param nickname the recipient's nickname
     * @return list of pending {@link Invitation} objects addressed to the player
     */
    public List<Invitation> getPendingInvitationsForPlayer(String nickname) {
        return invitations.values().stream()
                .filter(inv -> inv.getStatus() == Invitation.InvitationStatus.PENDING &&
                        inv.getToNickname().equals(nickname))
                .collect(Collectors.toList());
    }

    /**
     * Cancel all pending invitations where the given player is the sender or recipient.
     *
     * @param nickname the player's nickname
     */
    public void cancelInvitationsForPlayer(String nickname) {
        invitations.values().stream()
                .filter(inv -> inv.getStatus() == Invitation.InvitationStatus.PENDING &&
                        (inv.getFromNickname().equals(nickname) || inv.getToNickname().equals(nickname)))
                .forEach(inv -> {
                    inv.setStatus(Invitation.InvitationStatus.CANCELLED);
                    inv.setRespondedAt(LocalDateTime.now());
                });
    }

    /**
     * Mark all pending invitations older than 5 minutes as EXPIRED.
     * Intended to be called periodically to clean up stale invitations.
     */
    public void cleanupExpiredInvitations() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(5);
        invitations.values().stream()
                .filter(inv -> inv.getStatus() == Invitation.InvitationStatus.PENDING &&
                        inv.getCreatedAt().isBefore(expiryTime))
                .forEach(inv -> {
                    inv.setStatus(Invitation.InvitationStatus.EXPIRED);
                    inv.setRespondedAt(LocalDateTime.now());
                });
    }
}

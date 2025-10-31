package com.mastermind.service;

import com.mastermind.model.Invitation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class InvitationService {
    private final Map<String, Invitation> invitations = new ConcurrentHashMap<>();
    private final PlayerSessionService playerSessionService;

    public InvitationService(PlayerSessionService playerSessionService) {
        this.playerSessionService = playerSessionService;
    }

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

    public Invitation getInvitation(String invitationId) {
        return invitations.get(invitationId);
    }

    public List<Invitation> getPendingInvitationsForPlayer(String nickname) {
        return invitations.values().stream()
                .filter(inv -> inv.getStatus() == Invitation.InvitationStatus.PENDING &&
                        inv.getToNickname().equals(nickname))
                .collect(Collectors.toList());
    }

    public void cancelInvitationsForPlayer(String nickname) {
        invitations.values().stream()
                .filter(inv -> inv.getStatus() == Invitation.InvitationStatus.PENDING &&
                        (inv.getFromNickname().equals(nickname) || inv.getToNickname().equals(nickname)))
                .forEach(inv -> {
                    inv.setStatus(Invitation.InvitationStatus.CANCELLED);
                    inv.setRespondedAt(LocalDateTime.now());
                });
    }

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

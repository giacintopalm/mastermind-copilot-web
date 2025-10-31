package com.mastermind.service;

import com.mastermind.dto.PlayerListResponse;
import com.mastermind.model.PlayerSession;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class PlayerSessionService {
    // Thread-safe map to store active player sessions
    private final Map<String, PlayerSession> activeSessions = new ConcurrentHashMap<>();
    // Map nickname to sessionId for quick lookup
    private final Map<String, String> nicknameToSessionId = new ConcurrentHashMap<>();

    /**
     * Check if a nickname is already in use
     */
    public boolean isNicknameTaken(String nickname) {
        return nicknameToSessionId.containsKey(nickname.toLowerCase());
    }

    /**
     * Check if a nickname is connected (logged in)
     */
    public boolean isNicknameConnected(String nickname) {
        return nicknameToSessionId.containsKey(nickname.toLowerCase());
    }

    /**
     * Login a player with a unique nickname
     */
    public PlayerSession login(String nickname) {
        String normalizedNickname = nickname.trim();
        
        // Check if nickname is already taken
        if (isNicknameTaken(normalizedNickname)) {
            throw new IllegalArgumentException("Nickname '" + normalizedNickname + "' is already in use");
        }

        // Generate a unique session ID
        String sessionId = UUID.randomUUID().toString();
        
        // Create new player session
        PlayerSession session = new PlayerSession(sessionId, normalizedNickname);
        
        // Store the session
        activeSessions.put(sessionId, session);
        nicknameToSessionId.put(normalizedNickname.toLowerCase(), sessionId);
        
        return session;
    }

    /**
     * Logout a player by session ID
     */
    public void logout(String sessionId) {
        PlayerSession session = activeSessions.remove(sessionId);
        if (session != null) {
            nicknameToSessionId.remove(session.getNickname().toLowerCase());
        }
    }

    /**
     * Get a player session by session ID
     */
    public Optional<PlayerSession> getSession(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    /**
     * Get all active player sessions
     */
    public List<PlayerSession> getAllActivePlayers() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * Get list of active players (excluding the current player if sessionId provided)
     */
    public PlayerListResponse getPlayerList(String excludeSessionId) {
        List<PlayerListResponse.PlayerInfo> players = activeSessions.values().stream()
                .filter(session -> excludeSessionId == null || !session.getSessionId().equals(excludeSessionId))
                .map(PlayerListResponse.PlayerInfo::from)
                .sorted(Comparator.comparing(PlayerListResponse.PlayerInfo::getNickname))
                .collect(Collectors.toList());
        
        return new PlayerListResponse(players);
    }

    /**
     * Update player status
     */
    public void updatePlayerStatus(String sessionId, PlayerSession.PlayerStatus status) {
        PlayerSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setStatus(status);
        }
    }

    /**
     * Get total count of active players
     */
    public int getActivePlayerCount() {
        return activeSessions.size();
    }
}

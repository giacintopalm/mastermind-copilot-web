package com.mastermind.dto;

import com.mastermind.model.PlayerSession;
import java.util.List;

/**
 * Data Transfer Object for the list of active players in the lobby.
 * Contains metadata about each player such as session ID and current status.
 */
public class PlayerListResponse {
    private List<PlayerInfo> players;
    private int totalPlayers;

    public PlayerListResponse() {
    }

    public PlayerListResponse(List<PlayerInfo> players) {
        this.players = players;
        this.totalPlayers = players.size();
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
        this.totalPlayers = players != null ? players.size() : 0;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    /**
     * Lightweight summary of a single connected player.
     */
    public static class PlayerInfo {
        private String sessionId;
        private String nickname;
        private PlayerSession.PlayerStatus status;

        public PlayerInfo() {
        }

        public PlayerInfo(String sessionId, String nickname, PlayerSession.PlayerStatus status) {
            this.sessionId = sessionId;
            this.nickname = nickname;
            this.status = status;
        }

        /**
         * Construct a {@code PlayerInfo} from the given {@link PlayerSession}.
         *
         * @param session the active player session to summarise
         * @return a new {@code PlayerInfo} containing the session ID, nickname, and status
         */
        public static PlayerInfo from(PlayerSession session) {
            return new PlayerInfo(session.getSessionId(), session.getNickname(), session.getStatus());
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public PlayerSession.PlayerStatus getStatus() {
            return status;
        }

        public void setStatus(PlayerSession.PlayerStatus status) {
            this.status = status;
        }
    }
}

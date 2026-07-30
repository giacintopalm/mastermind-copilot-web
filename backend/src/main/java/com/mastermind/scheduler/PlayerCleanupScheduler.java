package com.mastermind.scheduler;

import com.mastermind.service.PlayerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that periodically removes inactive players from the lobby.
 * Broadcasts the updated player list via WebSocket after each cleanup run.
 */
@Component
public class PlayerCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PlayerCleanupScheduler.class);

    private final PlayerSessionService playerSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a {@code PlayerCleanupScheduler} with the required dependencies.
     *
     * @param playerSessionService the service used to identify and remove inactive players
     * @param messagingTemplate    the template used to broadcast the updated player list
     */
    @Autowired
    public PlayerCleanupScheduler(PlayerSessionService playerSessionService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.playerSessionService = playerSessionService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Runs every 2 minutes to remove players with no recent activity.
     * If any players are removed, broadcasts the updated player list to
     * all subscribers on {@code /topic/players}.
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void cleanupInactivePlayers() {
        int removed = playerSessionService.removeInactivePlayers();
        if (removed > 0) {
            logger.info("Removed {} inactive player(s)", removed);
            // Broadcast updated player list
            messagingTemplate.convertAndSend("/topic/players", playerSessionService.getPlayerList(null));
        }
    }
}

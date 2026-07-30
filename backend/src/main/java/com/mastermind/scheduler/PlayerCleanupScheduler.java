package com.mastermind.scheduler;

import com.mastermind.service.GameMatchService;
import com.mastermind.service.PlayerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlayerCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PlayerCleanupScheduler.class);

    private final PlayerSessionService playerSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameMatchService gameMatchService;

    @Autowired
    public PlayerCleanupScheduler(PlayerSessionService playerSessionService,
                                  SimpMessagingTemplate messagingTemplate,
                                  GameMatchService gameMatchService) {
        this.playerSessionService = playerSessionService;
        this.messagingTemplate = messagingTemplate;
        this.gameMatchService = gameMatchService;
    }

    /**
     * Run every 2 minutes to clean up inactive players
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void cleanupInactivePlayers() {
        // Cancel any active matches for players about to be removed
        playerSessionService.getInactivePlayerNicknames().forEach(nickname ->
            gameMatchService.cancelMatch(nickname)
        );

        int removed = playerSessionService.removeInactivePlayers();
        if (removed > 0) {
            logger.info("Removed {} inactive player(s)", removed);
            // Broadcast updated player list
            messagingTemplate.convertAndSend("/topic/players", playerSessionService.getPlayerList(null));
        }
    }
}

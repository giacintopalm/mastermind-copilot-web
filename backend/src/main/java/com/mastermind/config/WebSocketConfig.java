package com.mastermind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time multiplayer communication.
 * Enables STOMP messaging over WebSocket using SockJS fallback transport.
 * Clients subscribe to {@code /topic/*} destinations to receive push updates
 * such as player-list changes, invitations, and match state transitions.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the in-memory STOMP message broker.
     * Outbound messages are routed to {@code /topic} destinations; inbound
     * messages from clients must be prefixed with {@code /app}.
     *
     * @param config the message broker registry to configure
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages back to the client
        config.enableSimpleBroker("/topic");
        // Prefix for messages from the client to the server
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the {@code /ws} STOMP endpoint with SockJS fallback support.
     * Allowed origin patterns are explicitly enumerated for CORS safety.
     *
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint that clients will connect to
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                    "http://localhost:3000",
                    "http://localhost:3001", 
                    "http://localhost:5173",
                    "https://nice-sand-04c84f41e.1.azurestaticapps.net",
                    "https://mmgame.hyacinthwings.co.uk"
                )
                .withSockJS();
    }
}

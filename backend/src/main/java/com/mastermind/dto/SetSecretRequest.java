package com.mastermind.dto;

import java.util.List;

/**
 * Data Transfer Object for setting a player's secret code in a multiplayer match.
 * Contains an ordered list of color strings chosen by the player as their secret.
 */
public class SetSecretRequest {
    private List<String> secret;

    public SetSecretRequest() {
    }

    public SetSecretRequest(List<String> secret) {
        this.secret = secret;
    }

    public List<String> getSecret() {
        return secret;
    }

    public void setSecret(List<String> secret) {
        this.secret = secret;
    }
}

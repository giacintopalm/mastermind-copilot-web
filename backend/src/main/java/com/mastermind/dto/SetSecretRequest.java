package com.mastermind.dto;

import java.util.List;

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

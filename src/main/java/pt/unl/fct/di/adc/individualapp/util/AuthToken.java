package pt.unl.fct.di.adc.individualapp.util;

import java.util.UUID;

public class AuthToken {

    public static final long EXPIRATION_TIME = 1000 * 60 * 2; // Just 1 hours

    public String tokenId;
    public String userId;
    public String role;
    public long issuedAt;
    public long expiresAt;

    public AuthToken() {}

    public AuthToken(String userId, String role) {
        this.tokenId = UUID.randomUUID().toString();
        this.userId = userId;
        this.role = role;
        this.issuedAt = System.currentTimeMillis();
        this.expiresAt = this.issuedAt + EXPIRATION_TIME;
    }
}
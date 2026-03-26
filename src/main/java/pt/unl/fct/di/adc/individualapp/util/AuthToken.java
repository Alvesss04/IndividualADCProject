package pt.unl.fct.di.adc.individualapp.util;
import java.util.UUID;
public class AuthToken {

    // 15 minutes in milliseconds
    public static final long EXPIRATION_TIME = 900000;

    public String tokenId;
    public String username;
    public String role;
    public long issuedAt;
    public long expiresAt;

    public AuthToken() {}

    public AuthToken(String username, String role) {
        this.tokenId = UUID.randomUUID().toString();
        this.username = username;
        this.role = role;
        this.issuedAt = System.currentTimeMillis() / 1000;
        this.expiresAt = this.issuedAt + 900;
    }
}
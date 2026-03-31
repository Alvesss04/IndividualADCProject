package pt.unl.fct.di.adc.individualapp.util;
import java.util.UUID;
public class AuthToken {
    public static final long EXPIRATION_TIME = 900;

    public String tokenId;
    public String username;
    public String role;
    public long issuedAt;
    public long expiresAt;

    public AuthToken() {}

    public AuthToken(String username, String role) {
        this.tokenId   = UUID.randomUUID().toString();
        this.username  = username;
        this.role      = role;
        this.issuedAt  = System.currentTimeMillis() / 1000;
        this.expiresAt = this.issuedAt + EXPIRATION_TIME;
    }


    public boolean hasRole(Role r) {
        return Role.valueOf(this.role) == r;
    }

    public boolean hasAnyRole(Role... roles) {
        Role tokenRole = Role.valueOf(this.role);
        for (Role r : roles) {
            if (tokenRole == r) return true;
        }
        return false;
    }
}
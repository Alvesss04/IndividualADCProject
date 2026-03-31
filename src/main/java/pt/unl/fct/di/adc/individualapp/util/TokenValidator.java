package pt.unl.fct.di.adc.individualapp.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.JsonObject;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;


public class TokenValidator {

    public enum Status { OK, INVALID, EXPIRED }

    public Status status;
    public AuthToken token;
    public Key tokenKey;
    public ErrorCode error;

    private TokenValidator(Status status, AuthToken token, Key tokenKey, ErrorCode error) {
        this.status   = status;
        this.token    = token;
        this.tokenKey = tokenKey;
        this.error    = error;
    }

    public static TokenValidator validate(JsonObject tokenJson, Datastore datastore) {

        // 1. basic shape check
        if (tokenJson == null || !tokenJson.has("tokenId")) {
            return new TokenValidator(Status.INVALID, null, null, ErrorCode.INVALID_TOKEN);
        }

        String tokenId = tokenJson.get("tokenId").getAsString();
        if (tokenId == null || tokenId.isBlank()) {
            return new TokenValidator(Status.INVALID, null, null, ErrorCode.INVALID_TOKEN);
        }

        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(tokenId);
        Entity tokenEntity = datastore.get(tokenKey);

        if (tokenEntity == null) {
            return new TokenValidator(Status.INVALID, null, null, ErrorCode.INVALID_TOKEN);
        }

        AuthToken token = new AuthToken();
        token.tokenId   = tokenEntity.getString("tokenId");
        token.username  = tokenEntity.getString("username");
        token.role      = tokenEntity.getString("role");
        token.issuedAt  = tokenEntity.getLong("issuedAt");
        token.expiresAt = tokenEntity.getLong("expiresAt");

        long nowSeconds = System.currentTimeMillis() / 1000;
        if (nowSeconds > token.expiresAt) {
            return new TokenValidator(Status.EXPIRED, null, null, ErrorCode.TOKEN_EXPIRED);
        }

        return new TokenValidator(Status.OK, token, tokenKey, null);
    }

    public boolean isOk() {
        return status == Status.OK;
    }
}
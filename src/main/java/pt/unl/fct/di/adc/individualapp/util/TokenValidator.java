package pt.unl.fct.di.adc.individualapp.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

/**
 * Reusable token validator for all authenticated operations (Op3 - Op10).
 * Call validate() before doing any business logic in a resource.
 */
public class TokenValidator {

    // result wrapper so the caller knows what happened
    public enum Status { OK, INVALID, EXPIRED }

    public Status status;
    public AuthToken token; // populated only if status == OK
    public ErrorCode error; // populated only if status != OK

    private TokenValidator(Status status, AuthToken token, ErrorCode error) {
        this.status = status;
        this.token  = token;
        this.error  = error;
    }

    /**
     * Looks up the token in Datastore and checks expiry.
     *
     * @param tokenJson  the "token" JsonObject from the request
     * @param datastore  shared Datastore instance
     * @return           a TokenValidator result
     */
    public static TokenValidator validate(JsonObject tokenJson, Datastore datastore) {

        // 1. basic shape check
        if (tokenJson == null || !tokenJson.has("tokenId")) {
            return new TokenValidator(Status.INVALID, null, ErrorCode.INVALID_TOKEN);
        }

        String tokenId = tokenJson.get("tokenId").getAsString();
        if (tokenId == null || tokenId.isBlank()) {
            return new TokenValidator(Status.INVALID, null, ErrorCode.INVALID_TOKEN);
        }

        // 2. look up token in Datastore
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(tokenId);
        Entity tokenEntity = datastore.get(tokenKey);

        if (tokenEntity == null) {
            return new TokenValidator(Status.INVALID, null, ErrorCode.INVALID_TOKEN);
        }

        // 3. rebuild AuthToken from stored entity
        AuthToken token = new AuthToken();
        token.tokenId   = tokenEntity.getString("tokenId");
        token.username  = tokenEntity.getString("username");
        token.role      = tokenEntity.getString("role");
        token.issuedAt  = tokenEntity.getLong("issuedAt");
        token.expiresAt = tokenEntity.getLong("expiresAt");

        // 4. check expiry  (expiresAt is stored in seconds)
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (nowSeconds > token.expiresAt) {
            return new TokenValidator(Status.EXPIRED, null, ErrorCode.TOKEN_EXPIRED);
        }

        return new TokenValidator(Status.OK, token, null);
    }

    public boolean isOk() {
        return status == Status.OK;
    }
}
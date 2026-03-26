package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.AuthToken;
import pt.unl.fct.di.adc.individualapp.util.LoginData;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;
import java.util.logging.Logger;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public LoginResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        LoginData data = g.fromJson(request.input, LoginData.class);
        if (!data.isValid()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        LOG.fine("Login attempt for: " + data.username);

        // 1. look up the user in Datastore
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return buildError(ErrorCode.USER_NOT_FOUND);
        }

        // 2. verify password by comparing SHA-512 hashes
        String storedHash   = user.getString("password");
        String incomingHash = DigestUtils.sha512Hex(data.password);

        if (!storedHash.equals(incomingHash)) {
            return buildError(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. generate a new auth token
        String role = user.getString("role");
        AuthToken token = new AuthToken(data.username, role);

        // 4. persist the token in Datastore (kind = "Token", key = tokenId)
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(token.tokenId);
        Entity tokenEntity = Entity.newBuilder(tokenKey)
                .set("tokenId",   token.tokenId)
                .set("username",  token.username)
                .set("role",      token.role)
                .set("issuedAt",  token.issuedAt)
                .set("expiresAt", token.expiresAt)
                .build();

        datastore.put(tokenEntity);

        LOG.info("Login successful for: " + data.username);

        // 5. build and return the response
        JsonObject tokenJson = new JsonObject();
        tokenJson.addProperty("tokenId",   token.tokenId);
        tokenJson.addProperty("username",  token.username);
        tokenJson.addProperty("role",      token.role);
        tokenJson.addProperty("issuedAt",  token.issuedAt);
        tokenJson.addProperty("expiresAt", token.expiresAt);

        JsonObject dataObj = new JsonObject();
        dataObj.add("token", tokenJson);

        return buildSuccess(dataObj);
    }

    // { "status": "success", "data": { ... } }
    private Response buildSuccess(JsonObject data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", data);
        return Response.ok(g.toJson(response)).build();
    }

    // { "status": "<errorCode>", "data": "<message>" } — always HTTP 200 as per spec
    private Response buildError(ErrorCode error) {
        JsonObject response = new JsonObject();
        response.addProperty("status", error.name());
        response.addProperty("data", error.message);
        return Response.ok(g.toJson(response)).build();
    }
}
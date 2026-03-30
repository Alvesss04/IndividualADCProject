package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogOutResource {

    private static final Logger LOG = Logger.getLogger(LogOutResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public LogOutResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logOutResource(String body) {

        // parse the raw request body
        OperationRequest request = OperationRequest.fromJson(body);

        // token is required for logout
        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        // input with username is also required
        if (request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        // validate the token against Datastore (checks existence + expiry)
        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        // get the target username from input
        String targetUsername = request.input.get("username").getAsString();

        // only ADMIN can logout anyone, USER and BOFFICER can only logout their own session
        if (!tv.token.role.equals("ADMIN") && !tv.token.username.equals(targetUsername)) {
            return buildError(ErrorCode.FORBIDDEN);
        }

        // get the tokenId to find and delete the token entity in Datastore
        String tokenId = request.token.get("tokenId").getAsString();

        // find the token entity in Datastore using its tokenId as the key
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(tokenId);
        Entity tokenEntity = datastore.get(tokenKey);

        // if token doesn't exist in DB it's already invalid
        if (tokenEntity == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        // delete the token — this officially ends the authenticated session
        datastore.delete(tokenKey);

        LOG.info("User " + targetUsername + " logged out successfully");

        // return success response as per spec
        JsonObject responseData = new JsonObject();
        responseData.addProperty("message", "Logout successful");
        return buildSuccess(responseData);
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
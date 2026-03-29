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

@Path("/showuserrole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUserRoleResource {

    private static final Logger LOG = Logger.getLogger(ShowUserRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public ShowUserRoleResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUserRole(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        if (request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        if (!tv.token.role.equals("ADMIN") && !tv.token.role.equals("BOFFICER")) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        String targetUsername = null;
        if (request.input.has("username")) {
            targetUsername = request.input.get("username").getAsString();
        }

        if (targetUsername == null || targetUsername.isBlank() || !targetUsername.contains("@")) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        LOG.fine("ShowUserRole requested by: " + tv.token.username + " targeting: " + targetUsername);

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return buildError(ErrorCode.USER_NOT_FOUND);
        }

        if (tv.token.role.equals("BOFFICER") && user.getString("role").equals("ADMIN")) {
            return buildError(ErrorCode.FORBIDDEN);
        }

        LOG.info("ShowUserRole returned role for: " + targetUsername);


        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("username", targetUsername);
        dataObj.addProperty("role", user.getString("role"));

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
package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUsersResource {

    private static final Logger LOG = Logger.getLogger(ShowUsersResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public ShowUsersResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUsers(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        // check wrapper arrived
        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        // 1. validate token (exists in DB + not expired)
        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        // 2. check role — only ADMIN and BOFFICER are allowed
        String role = tv.token.role;
        if (!role.equals("ADMIN") && !role.equals("BOFFICER")) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        LOG.fine("ShowUsers requested by: " + tv.token.username + " (" + role + ")");

        // 3. query all User entities from Datastore
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .build();

        QueryResults<Entity> results = datastore.run(query);

        // 4. build the users array
        JsonArray usersArray = new JsonArray();
        while (results.hasNext()) {
            Entity user = results.next();
            JsonObject userJson = new JsonObject();
            userJson.addProperty("username", user.getString("username"));
            userJson.addProperty("role",     user.getString("role"));
            usersArray.add(userJson);
        }

        JsonObject dataObj = new JsonObject();
        dataObj.add("users", usersArray);

        LOG.info("ShowUsers returned " + usersArray.size() + " users to: " + tv.token.username);

        return buildSuccess(dataObj);
    }

    private Response buildSuccess(JsonObject data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", data);
        return Response.ok(g.toJson(response)).build();
    }

    private Response buildError(ErrorCode error) {
        JsonObject response = new JsonObject();
        response.addProperty("status", error.name());
        response.addProperty("data", error.message);
        return Response.ok(g.toJson(response)).build();
    }
}
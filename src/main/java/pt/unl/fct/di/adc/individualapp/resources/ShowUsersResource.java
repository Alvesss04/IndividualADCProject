package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.Role;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON)
public class ShowUsersResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(ShowUsersResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ShowUsersResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUsers(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        if (!tv.token.hasAnyRole(Role.ADMIN, Role.BOFFICER)) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        LOG.fine("ShowUsers requested by: " + tv.token.username + " (" + tv.token.role + ")");

        Query<Entity> query = Query.newEntityQueryBuilder().setKind("User").build();

        QueryResults<Entity> results = datastore.run(query);

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
}
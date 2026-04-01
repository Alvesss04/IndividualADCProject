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

@Path("/showauthsessions")
@Produces(MediaType.APPLICATION_JSON)
public class ShowAuthSessionsResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(ShowAuthSessionsResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ShowAuthSessionsResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showAuthSessions(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        if (!tv.token.hasRole(Role.ADMIN)) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        LOG.fine("ShowAuthSessions requested by: " + tv.token.username);

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .build();

        QueryResults<Entity> results = datastore.run(query);

        JsonArray sessionsArray = new JsonArray();
        while (results.hasNext()) {
            Entity token = results.next();
            JsonObject sessionJson = new JsonObject();
            sessionJson.addProperty("tokenId",   token.getString("tokenId"));
            sessionJson.addProperty("username",  token.getString("username"));
            sessionJson.addProperty("role",      token.getString("role"));
            sessionJson.addProperty("expiresAt", token.getLong("expiresAt"));
            sessionsArray.add(sessionJson);
        }

        JsonObject dataObj = new JsonObject();
        dataObj.add("sessions", sessionsArray);

        LOG.info("ShowAuthSessions returned " + sessionsArray.size() + " sessions to: " + tv.token.username);

        return buildSuccess(dataObj);
    }
}
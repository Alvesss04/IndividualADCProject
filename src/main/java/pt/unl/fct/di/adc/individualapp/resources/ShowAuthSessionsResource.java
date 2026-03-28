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


@Path("/showauthsessions")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowAuthSessionsResource {

    private static final Logger LOG = Logger.getLogger(ShowAuthSessionsResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public ShowAuthSessionsResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response ShowAuthSessions(String body){
        OperationRequest request = OperationRequest.fromJson(body);
        if (request == null || request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        TokenValidator tv = TokenValidator.validate(request.token,datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        if (!tv.token.role.equals("ADMIN")){
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        LOG.fine("ShowAuthSessions requested by: " + tv.token.username);

        // Query is used to archive every Token (only tokens)
        Query<Entity> query = Query.newEntityQueryBuilder().setKind("Token").build();
        //Then here we save the results in a querry so we can run 1 by 1, done by (datastore.run)
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

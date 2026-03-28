package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/deleteaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DeleteAccountResource {

    private static final Logger LOG = Logger.getLogger(DeleteAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public DeleteAccountResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteAccount(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        // check wrapper and token arrived
        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        // check input arrived
        if (request.input == null || !request.input.has("username")) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        // 1. validate token (exists in DB + not expired)
        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        // 2. only ADMIN can delete accounts
        if (!tv.token.role.equals("ADMIN")) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        String targetUsername = request.input.get("username").getAsString();

        if (targetUsername == null || targetUsername.isBlank()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        // 3. ADMIN cannot delete their own account
        if (tv.token.username.equals(targetUsername)) {
            return buildError(ErrorCode.FORBIDDEN);
        }

        LOG.fine("DeleteAccount requested by: " + tv.token.username + " targeting: " + targetUsername);

        // 4. check target user exists
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return buildError(ErrorCode.USER_NOT_FOUND);
        }

        // 5. find and delete ALL tokens belonging to the target user
        Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", targetUsername))
                .build();

        QueryResults<Entity> tokenResults = datastore.run(tokenQuery);

        List<Key> tokenKeysToDelete = new ArrayList<>();
        while (tokenResults.hasNext()) {
            tokenKeysToDelete.add(tokenResults.next().getKey());
        }

        // 6. delete user + all their tokens in a transaction
        Transaction txn = datastore.newTransaction();
        try {
            // delete all tokens first
            if (!tokenKeysToDelete.isEmpty()) {
                txn.delete(tokenKeysToDelete.toArray(new Key[0]));
            }

            // delete the user account
            txn.delete(userKey);
            txn.commit();

            LOG.info("Account deleted: " + targetUsername + " (tokens removed: " + tokenKeysToDelete.size() + ")");

            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("message", "Account deleted successfully");

            return buildSuccess(dataObj);

        }catch (Exception e){
            txn.rollback();
            LOG.severe("Error deleting account: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
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
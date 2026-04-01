package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.ChangeUserRoleData;
import pt.unl.fct.di.adc.individualapp.util.Role;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/changeuserrole")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeUserRoleResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(ChangeUserRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ChangeUserRoleResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserRole(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }
        if (request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        ChangeUserRoleData data = g.fromJson(request.input, ChangeUserRoleData.class);

        if (!data.isValid()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        if (!tv.token.hasRole(Role.ADMIN)) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        if (tv.token.username.equals(data.username)) {
            return buildError(ErrorCode.FORBIDDEN);
        }

        LOG.fine("ChangeUserRole requested by: " + tv.token.username + " targeting: " + data.username);

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity targetUser = datastore.get(userKey);

        if (targetUser == null) return buildError(ErrorCode.USER_NOT_FOUND);

        Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", data.username))
                .build();

        QueryResults<Entity> tokenResults = datastore.run(tokenQuery);
        List<Key> tokenKeys = new ArrayList<>();
        while (tokenResults.hasNext()) {
            tokenKeys.add(tokenResults.next().getKey());
        }

        Transaction newTxn = datastore.newTransaction();
        try {
            Entity updatedUser = Entity.newBuilder(targetUser)
                    .set("role", data.newRole.toUpperCase())
                    .build();
            newTxn.put(updatedUser);

            for (Key tokenKey : tokenKeys) {
                Entity tokenEntity = newTxn.get(tokenKey);
                if (tokenEntity != null) {
                    Entity updatedToken = Entity.newBuilder(tokenEntity)
                            .set("role", data.newRole.toUpperCase())
                            .build();
                    newTxn.put(updatedToken);
                }
            }
            newTxn.commit();
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("message", "Role updated successfully");
            return buildSuccess(dataObj);

        } catch (Exception e) {
            newTxn.rollback();
            LOG.severe("Error changing role: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            if (newTxn.isActive()) newTxn.rollback();
        }
    }
}
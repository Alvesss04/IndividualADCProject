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

import java.util.logging.Logger;

@Path("/changeuserrole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
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

        Transaction txn = datastore.newTransaction();
        try {
            Entity updatedUser = Entity.newBuilder(targetUser)
                    .set("role", data.newRole.toUpperCase())
                    .build();

            txn.put(updatedUser);
            txn.commit();

            LOG.info("Role updated for: " + data.username + " to: " + data.newRole.toUpperCase());

            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("message", "Role updated successfully");
            return buildSuccess(dataObj);

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error changing role: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}
package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.Role;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/showuserrole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUserRoleResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(ShowUserRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

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

        if (!tv.token.hasAnyRole(Role.ADMIN, Role.BOFFICER)) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        String targetUsername = request.input.has("username")
                ? request.input.get("username").getAsString()
                : null;

        if (targetUsername == null || targetUsername.isBlank() || !targetUsername.contains("@")) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        LOG.fine("ShowUserRole requested by: " + tv.token.username + " targeting: " + targetUsername);

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return buildError(ErrorCode.USER_NOT_FOUND);
        }

        if (tv.token.hasRole(Role.BOFFICER) && user.getString("role").equals(Role.ADMIN.name())) {
            return buildError(ErrorCode.FORBIDDEN);
        }

        LOG.info("ShowUserRole returned role for: " + targetUsername);

        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("username", targetUsername);
        dataObj.addProperty("role", user.getString("role"));

        return buildSuccess(dataObj);
    }
}
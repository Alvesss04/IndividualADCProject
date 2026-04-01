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

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON)
public class LogOutResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(LogOutResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public LogOutResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logout(String body) {

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

        String targetUsername = request.input.has("username")
                ? request.input.get("username").getAsString()
                : null;

        if (targetUsername == null || targetUsername.isBlank()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        if (!tv.token.hasRole(Role.ADMIN) && !tv.token.username.equals(targetUsername)) {
            return buildError(ErrorCode.FORBIDDEN);
        }

        datastore.delete(tv.tokenKey);

        LOG.info("User " + targetUsername + " logged out successfully");

        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("message", "Logout successful");
        return buildSuccess(dataObj);
    }
}
package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.ChangeUserPasswordData;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/changeuserpwd")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeUserPasswordResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(ChangeUserPasswordResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ChangeUserPasswordResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeUserPwd(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        if (request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        ChangeUserPasswordData data = g.fromJson(request.input, ChangeUserPasswordData.class);

        if (!data.isValid()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);

        if (user == null) return buildError(ErrorCode.USER_NOT_FOUND);

        if (!tv.token.username.equals(data.username)) {
            return buildError(ErrorCode.FORBIDDEN);
        }

        String storedHash   = user.getString("password");
        String incomingHash = DigestUtils.sha512Hex(data.oldPassword);

        if (!storedHash.equals(incomingHash)) {
            return buildError(ErrorCode.INVALID_CREDENTIALS);
        }

        Transaction txn = datastore.newTransaction();
        try {
            Entity updatedUser = Entity.newBuilder(user)
                    .set("password", DigestUtils.sha512Hex(data.newPassword))
                    .build();

            txn.put(updatedUser);
            txn.commit();

            LOG.info("Password changed for: " + data.username);

            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("message", "Password changed successfully");
            return buildSuccess(dataObj);

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error changing password: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}
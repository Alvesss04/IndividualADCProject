package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.ModifyAccountData;
import pt.unl.fct.di.adc.individualapp.util.Role;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/modaccount")
@Produces(MediaType.APPLICATION_JSON)
public class ModifyAccountResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(ModifyAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ModifyAccountResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyAccount(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.token == null) {
            return buildError(ErrorCode.INVALID_TOKEN);
        }

        if (request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        ModifyAccountData data = g.fromJson(request.input, ModifyAccountData.class);

        if (!data.isValid()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        String callerUsername = tv.token.username;
        String targetUsername = data.username;

        Entity cachedTargetUser = null;

        if (tv.token.hasRole(Role.USER)) {
            if (!callerUsername.equals(targetUsername)) {
                return buildError(ErrorCode.FORBIDDEN);
            }

        } else if (tv.token.hasRole(Role.BOFFICER)) {
            if (!callerUsername.equals(targetUsername)) {
                Key targetKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
                cachedTargetUser = datastore.get(targetKey);
                if (cachedTargetUser == null) return buildError(ErrorCode.USER_NOT_FOUND);
                if (!cachedTargetUser.getString("role").equals(Role.USER.name())) {
                    return buildError(ErrorCode.FORBIDDEN);
                }
            }

        } else if (!tv.token.hasRole(Role.ADMIN)) {
            return buildError(ErrorCode.UNAUTHORIZED);
        }

        LOG.fine("ModifyAccount requested by: " + callerUsername + " targeting: " + targetUsername);

        Entity user = cachedTargetUser != null
                ? cachedTargetUser
                : datastore.get(datastore.newKeyFactory().setKind("User").newKey(targetUsername));

        if (user == null) return buildError(ErrorCode.USER_NOT_FOUND);

        Entity.Builder updatedUser = Entity.newBuilder(user);

        if (data.attributes.phone != null && !data.attributes.phone.isBlank()) {
            updatedUser.set("phone", data.attributes.phone);
        }
        if (data.attributes.address != null && !data.attributes.address.isBlank()) {
            updatedUser.set("address", data.attributes.address);
        }


        Transaction newTxn = datastore.newTransaction();
        try {
            newTxn.put(updatedUser.build());
            newTxn.commit();

            LOG.info("Account modified: " + targetUsername + " by: " + callerUsername);

            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("message", "Updated successfully");
            return buildSuccess(dataObj);

        } catch (Exception e) {
            newTxn.rollback();
            LOG.severe("Error modifying account: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            if (newTxn.isActive()) newTxn.rollback();
        }
    }
}
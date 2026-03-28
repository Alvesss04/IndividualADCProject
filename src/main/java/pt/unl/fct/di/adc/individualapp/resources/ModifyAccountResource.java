package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.ModifyAccountData;
import pt.unl.fct.di.adc.individualapp.util.TokenValidator;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/modifyaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ModifyAccountResource {
    private static final Logger LOG = Logger.getLogger(ModifyAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public ModifyAccountResource(){}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyAccount(String body) {

        OperationRequest request = OperationRequest.fromJson(body);
        if (request == null || request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        ModifyAccountData data = g.fromJson(request.input, ModifyAccountData.class);

        if (!data.isValid()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        LOG.fine("Attempting to create account for: " + data.username);

        TokenValidator tv = TokenValidator.validate(request.token, datastore);
        if (!tv.isOk()) {
            return buildError(tv.error);
        }

        String callerRole     = tv.token.role;
        String callerUsername = tv.token.username;
        String targetUsername = data.username;

        switch (callerRole) {
            case "USER":
                // USER can only modify their own account
                if (!callerUsername.equals(targetUsername)) {
                    return buildError(ErrorCode.FORBIDDEN);
                }
                break;

            case "BOFFICER":
                if (!callerUsername.equals(targetUsername)) {
                    Key targetKey = datastore.newKeyFactory().setKind("User").newKey(targetUsername);
                    Entity targetUser = datastore.get(targetKey);
                    if (targetUser == null) {
                        return buildError(ErrorCode.USER_NOT_FOUND);
                    }
                    if (!targetUser.getString("role").equals("USER")) {
                        return buildError(ErrorCode.FORBIDDEN);
                    }
                }
                break;
            case "ADMIN":
                break;
            default:
                return buildError(ErrorCode.UNAUTHORIZED);
        }

        LOG.fine("ModifyAccount requested by: " + callerUsername + " targeting: " + targetUsername);


        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return buildError(ErrorCode.USER_NOT_FOUND);
        }

        //Function to overwrite the new modified acc
        Entity.Builder updatedUser = Entity.newBuilder(user);

        if (data.attributes.phone != null && !data.attributes.phone.isBlank()) {
            updatedUser.set("phone", data.attributes.phone);
        }
        if (data.attributes.address != null && !data.attributes.address.isBlank()) {
            updatedUser.set("address", data.attributes.address);
        }

        Transaction txn = datastore.newTransaction();
        try {
            txn.put(updatedUser.build());
            txn.commit();

            LOG.info("Account modified: " + targetUsername + " by: " + callerUsername);

            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("message", "Updated successfully");

            return buildSuccess(dataObj);

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error modifying account: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
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


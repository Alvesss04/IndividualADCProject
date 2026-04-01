package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.CreateAccountData;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/createaccount")
@Produces(MediaType.APPLICATION_JSON)
public class CreateAccountResource extends BaseResource {

    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public CreateAccountResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(String body) {

        OperationRequest request = OperationRequest.fromJson(body);

        if (request == null || request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        CreateAccountData data = g.fromJson(request.input, CreateAccountData.class);

        if (!data.isValid()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        LOG.fine("Attempting to create account for: " + data.username);

        Transaction newTxn = datastore.newTransaction();
        try {
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);

            Entity existing = newTxn.get(userKey);
            if (existing != null) {
                newTxn.rollback();
                return buildError(ErrorCode.USER_ALREADY_EXISTS);
            }

            Entity user = Entity.newBuilder(userKey)
                    .set("username",     data.username)
                    .set("password",     DigestUtils.sha512Hex(data.password))
                    .set("phone",        data.phone)
                    .set("address",      data.address)
                    .set("role",         data.role.toUpperCase())
                    .set("creationTime", Timestamp.now())
                    .build();

            newTxn.put(user);
            newTxn.commit();

            LOG.info("Account created for: " + data.username);

            JsonObject responseData = new JsonObject();
            responseData.addProperty("username", data.username);
            responseData.addProperty("role",     data.role.toUpperCase());

            return buildSuccess(responseData);

        } catch (Exception e) {
            newTxn.rollback();
            LOG.severe("Error creating account: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            if (newTxn.isActive()) newTxn.rollback();
        }
    }
}
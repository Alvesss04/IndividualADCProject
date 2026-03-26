package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.individualapp.util.CreateAccountData;
import pt.unl.fct.di.adc.individualapp.input.OperationRequest;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

import java.util.logging.Logger;

@Path("/createaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class CreateAccountResource {

    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public CreateAccountResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(String body) {

        OperationRequest request = OperationRequest.fromJson(body);        // check the wrapper and input arrived
        if (request == null || request.input == null) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        // parse the "input" JsonObject into our CreateAccountData class
        CreateAccountData data = g.fromJson(request.input, CreateAccountData.class);

        // validate all fields
        if (!data.isValid()) {
            return buildError(ErrorCode.INVALID_INPUT);
        }

        LOG.fine("Attempting to create account for: " + data.username);

        Transaction txn = datastore.newTransaction();

        try {
            // username (email format) is the unique key in Datastore
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);

            // check if username already exists
            Entity existing = txn.get(userKey);
            if (existing != null) {
                txn.rollback();
                return buildError(ErrorCode.USER_ALREADY_EXISTS);
            }

            // build and store the new user entity
            Entity user = Entity.newBuilder(userKey)
                    .set("username", data.username)
                    .set("password", DigestUtils.sha512Hex(data.password)) // always hash passwords
                    .set("phone", data.phone)
                    .set("address", data.address)
                    .set("role", data.role.toUpperCase())
                    .set("creationTime", Timestamp.now())
                    .build();

            txn.put(user);
            txn.commit();

            LOG.info("Account created for: " + data.username);

            // success response as per spec
            JsonObject responseData = new JsonObject();
            responseData.addProperty("username", data.username);
            responseData.addProperty("role", data.role.toUpperCase());

            return buildSuccess(responseData);

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error creating account: " + e.getMessage());
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
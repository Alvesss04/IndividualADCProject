package pt.unl.fct.di.adc.individualapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.individualapp.util.CreateAccountData;

@Path("/createaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class CreateAccountResource {

    // logger for tracking what happens on the server
    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());

    // single shared connection to Firestore/Datastore
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    // Gson converts Java objects to/from JSON
    private final Gson g = new Gson();

    public CreateAccountResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON) // we expect JSON in the request body
    public Response createAccount(String requestBody) {

        // parse the outer JSON to get the "input" object inside
        JsonObject json = g.fromJson(requestBody, JsonObject.class);

        // check that the "input" field actually exists in the request
        if (json == null || !json.has("input")) {
            return buildError("INVALID_INPUT", "The call is using input data not following the correct specification");
        }

        // extract the "input" object and parse it into our data class
        CreateAccountData data = g.fromJson(json.get("input"), CreateAccountData.class);

        // validate all fields using the isValid() method we wrote
        if (!data.isValid()) {
            return buildError("INVALID_INPUT", "The call is using input data not following the correct specification");
        }

        LOG.fine("Attempting to create account for: " + data.username);

        // use a transaction so no two users can register with the same username at the same time
        Transaction txn = datastore.newTransaction();

        try {
            // build the key using username as the unique identifier (primary key)
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);

            // check if a user with this username already exists
            Entity existing = txn.get(userKey);
            if (existing != null) {
                txn.rollback(); // cancel the transaction
                return buildError("USER_ALREADY_EXISTS", "Error in creating an account because the username already exists");
            }

            // build the new user entity with all fields
            Entity user = Entity.newBuilder(userKey)
                    .set("username", data.username)
                    .set("password", DigestUtils.sha512Hex(data.password)) // hash the password, never store plain text
                    .set("email", data.email)
                    .set("phone", data.phone)
                    .set("address", data.address)
                    .set("role", data.role.toUpperCase()) // always store role in uppercase
                    .set("creationTime", Timestamp.now()) // record when the account was created
                    .build();

            // save the user to the database
            txn.put(user);
            txn.commit(); // confirm and finalize the transaction

            LOG.info("Account created successfully for: " + data.username);

            // build the success response as specified: return username and role
            JsonObject responseData = new JsonObject();
            responseData.addProperty("username", data.username);
            responseData.addProperty("role", data.role.toUpperCase());

            return buildSuccess(responseData);

        } catch (Exception e) {
            txn.rollback(); // if anything goes wrong, undo everything
            LOG.severe("Error creating account: " + e.getMessage());
            return Response.serverError().build();
        } finally {
            // safety net: if transaction is still open for any reason, roll it back
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    // helper: builds a standard success response
    // { "status": "success", "data": { ... } }
    private Response buildSuccess(JsonObject data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", data);
        return Response.ok(g.toJson(response)).build();
    }

    // helper: builds a standard error response
    // { "status": "<error>", "data": "<message>" }
    private Response buildError(String errorCode, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", errorCode);
        response.addProperty("data", message);
        return Response.ok(g.toJson(response)).build();
    }
}
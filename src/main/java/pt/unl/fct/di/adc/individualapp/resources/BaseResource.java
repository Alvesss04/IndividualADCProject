package pt.unl.fct.di.adc.individualapp.resources;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.individualapp.util.exceptions.ErrorCode;

public abstract class BaseResource {

    protected final Gson g = new Gson();

    // { "status": "success", "data": { ... } }
    protected Response buildSuccess(JsonObject data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", data);
        return Response.ok(g.toJson(response)).build();
    }

    // { "status": "<errorCode>", "data": "<message>" } — always HTTP 200 as per spec
    protected Response buildError(ErrorCode error) {
        JsonObject response = new JsonObject();
        response.addProperty("status", error.name());
        response.addProperty("data", error.message);
        return Response.ok(g.toJson(response)).build();
    }
}

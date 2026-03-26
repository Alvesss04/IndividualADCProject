package pt.unl.fct.di.adc.individualapp.input;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

// generic request wrapper for ALL operations
// matches the structure: { "input": { ... }, "token": { ... } }
public class OperationRequest {

    public JsonObject input; // present in all operations
    public JsonObject token; // only present in authenticated operations (Op3-Op10)

    public OperationRequest() {}

    public static OperationRequest fromJson(String body) {
        Gson g = new Gson();
        return g.fromJson(body, OperationRequest.class);
    }
}

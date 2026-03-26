package pt.unl.fct.di.adc.individualapp.resources;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.gson.Gson;

import java.util.logging.Logger;

public class LoginResource {

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    // single shared connection to Firestore/Datastore
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    // Gson converts Java objects to/from JSON
    private final Gson g = new Gson();

    public LoginResource() {}

}

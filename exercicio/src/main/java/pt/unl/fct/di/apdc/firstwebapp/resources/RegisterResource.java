package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import org.apache.commons.codec.digest.DigestUtils;

import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public RegisterResource() {
    } // Nothing to do here

    @POST
    @Path("/v3")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerV3(RegisterData data) {
        LOG.fine("Attempt to register user: " + data.username);
        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
            Entity user = txn.get(userKey);
            if (user != null) {
                txn.rollback();
                return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
            } else {
                user = Entity.newBuilder(userKey).set("user_name", data.name).set("user_pwd", DigestUtils.sha512Hex(data.password)).set("user_email", data.email).set("user_role", data.role).set("user_state", data.state).set("user_privacy", data.privacy).build();
            }
            txn.add(user);
            LOG.info("User registered " + data.username);
            txn.commit();
            return Response.ok().build();
        } catch (Exception e) {
            txn.rollback();
            LOG.severe(e.getMessage());
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/SU")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerSU(RegisterData data) {
        data.state = "Active";
        data.role = "SU";
        return registerV3(data);
    }
}
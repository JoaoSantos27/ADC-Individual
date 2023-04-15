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

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public LogoutResource() {
    } // Nothing to do here

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLogoutV1(AuthToken data) {
        LOG.fine("Attempting to logout user: " + data.username);
        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.tokenID);
        Transaction txn = datastore.newTransaction();
        try {
            Entity token = txn.get(tokenKey);
            if (token == null) {
                // Token does not exist
                LOG.warning("Token does not exist");
                return Response.status(Status.FORBIDDEN).build();
            }
            if (data.username.equals(token.getString("token_name"))) {
                txn.delete(tokenKey);
                txn.commit();
                return Response.ok().build();
            }
            LOG.warning("Token does not belong to you");
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            LOG.severe(e.getMessage());
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }

    }
}
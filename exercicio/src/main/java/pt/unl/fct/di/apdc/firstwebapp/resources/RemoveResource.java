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
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.RemoveData;

@Path("/remove")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RemoveResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private static final Gson g = new Gson();

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public RemoveResource() {
    } // Nothing to do here

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response doRemoveV1(RemoveData data) {
        LOG.fine("Attempting to remove user: " + data.username);
        // Construct the key from the username
        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.token.tokenID);
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Key ctrsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username)).setKind("UserStats").newKey("counters");
        Transaction txn = datastore.newTransaction();
        try {
            Entity token = txn.get(tokenKey);
            if (token == null) {
                // Token does not exist
                LOG.warning("Token does not exist");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            if (data.token.username.equals(token.getString("token_name"))) {
                Entity user = txn.get(userKey);
                if (user == null) {
                    // Username does not exist
                    txn.rollback();
                    LOG.warning("Failed remove attempt for username: " + data.username);
                    return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
                }
                if (checkPermissions(data, user.getString("user_role"))) {
                    Entity stats = txn.get(ctrsKey);
                    if (stats != null) {
                        txn.delete(ctrsKey);
                    }
                    txn.delete(userKey);
                    if (data.token.username.equals(data.username)) {
                        txn.delete(tokenKey);
                        txn.commit();
                        return Response.ok().build();
                    }
                    txn.commit();
                    return Response.ok(g.toJson(data.token)).build();
                } else {
                    txn.rollback();
                    LOG.warning("You don't have permission to delete that user.");
                    return Response.status(Status.FORBIDDEN).build();
                }
            } else {
                LOG.warning("Token does not belong to you");
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            txn.rollback();
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

    private boolean checkPermissions(RemoveData data, String role) {
        return (data.token.username.equals(data.username)) || (data.token.role.equals("GBO") && role.equals("User")) || (data.token.role.equals("GA") && (role.equals("User") || role.equals("GBO"))) || (data.token.role.equals("GS") && (role.equals("User") || role.equals("GBO") || role.equals("GS")) || (data.token.role.equals("SU")));
    }
}
package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.List;
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
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.StatsData;
import pt.unl.fct.di.apdc.firstwebapp.util.UserData;

import com.google.gson.Gson;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ListResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private static final Gson g = new Gson();

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ListResource() {
    } // Nothing to do here

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response doListV1(AuthToken data) {
        LOG.fine("Attempting to list all users");
        String role = data.role;
        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.tokenID);
        Transaction txn = datastore.newTransaction();
        try {
            Entity token = txn.get(tokenKey);
            if (token == null) {
                // Token does not exist
                LOG.warning("Token does not exist");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            if (data.username.equals(token.getString("token_name"))) {
                // Construct the query based on the role
                Query<Entity> query;
                if (role.equals("User")) {
                    query = Query.newEntityQueryBuilder().setKind("User").setFilter(CompositeFilter.and(PropertyFilter.eq("user_role", "User"), PropertyFilter.eq("user_state", "Active"))).build();
                } else if (role.equals("GBO")) {
                    query = Query.newEntityQueryBuilder().setKind("User").setFilter(PropertyFilter.eq("user_role", "User")).build();
                } else if (role.equals("GA")) {
                    query = Query.newEntityQueryBuilder().setKind("User").setFilter(PropertyFilter.eq("user_role", ListValue.of("User", "GBO"))).build();
                } else if (role.equals("GS")) {
                    query = Query.newEntityQueryBuilder().setKind("User").setFilter(PropertyFilter.eq("user_role", ListValue.of("User", "GBO", "GA"))).build();
                } else if (role.equals("SU")) {
                    query = Query.newEntityQueryBuilder().setKind("User").build();
                } else {
                    // Invalid role
                    return Response.status(Status.BAD_REQUEST).build();
                }

                // Execute the query and retrieve the results
                List<UserData> users = new ArrayList<>();
                QueryResults<Entity> queryResults = datastore.run(query);
                while (queryResults.hasNext()) {
                    Entity entity = queryResults.next();
                    users.add(entityToUser(entity));
                }
                txn.commit();
                // Serialize the list of users to JSON and return as response
                return Response.ok(g.toJson(users)).build();
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

    @POST
    @Path("/stats")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response doStatsV1(AuthToken data) {
        LOG.fine("Attempting to list all users");
        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.tokenID);
        Key ctrsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username)).setKind("UserStats").newKey("counters");
        Transaction txn = datastore.newTransaction();
        try {
            Entity token = txn.get(tokenKey);
            if (token == null) {
                // Token does not exist
                LOG.warning("Token does not exist");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            if (data.username.equals(token.getString("token_name"))) {
                // Construct the query based on the role
                Entity stats = txn.get(ctrsKey);
                txn.commit();
                return Response.ok(g.toJson(entityToStats(stats))).build();
            } else {
                txn.rollback();
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

    @POST
    @Path("/info")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response doInfoV1(AuthToken data) {
        LOG.fine("Attempting to list all users");
        String role = data.role;
        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.tokenID);
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Transaction txn = datastore.newTransaction();
        try {
            Entity token = txn.get(tokenKey);
            if (token == null) {
                // Token does not exist
                LOG.warning("Token does not exist");
                return Response.status(Status.UNAUTHORIZED).build();
            }
            if (data.username.equals(token.getString("token_name"))) {
                Entity entity = txn.get(userKey);
                UserData user = entityToUser(entity);
                txn.commit();
                return Response.ok(g.toJson(user)).build();
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

    private UserData entityToUser(Entity entity) {
        String username = entity.getKey().getName();
        String email = entity.getString("user_email");
        String name = entity.getString("user_name");
        String privacy = entity.getString("user_privacy");
        String role = entity.getString("user_role");
        String state = entity.getString("user_state");
        return new UserData(username, email, name, privacy, role, state);
    }

    private StatsData entityToStats(Entity entity) {
        long logins = entity.getLong("user_stats_logins");
        long failedLogins = entity.getLong("user_stats_failed");
        String firstLogin = entity.getTimestamp("user_first_login").toDate().toString();
        String lastLogin = entity.getTimestamp("user_last_login").toDate().toString();
        String lastAttempt = null;
        if (entity.contains("user_last_attempt")) {
            lastAttempt = entity.getTimestamp("user_last_attempt").toDate().toString();
        }
        return new StatsData(logins, failedLogins, firstLogin, lastLogin, lastAttempt);
    }
}
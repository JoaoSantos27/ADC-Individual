package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public LoginResource() {
    } // Nothing to do here

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response doLoginV1(LoginData data, @Context HttpServletRequest request, @Context HttpHeaders headers) {
        LOG.fine("Attempting to login user: " + data.username);
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Key ctrsKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", data.username)).setKind("UserStats").newKey("counters");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                LOG.warning("Failed login attempt for username: " + data.username);
                return Response.status(Status.FORBIDDEN).build();
            }

            Entity stats = txn.get(ctrsKey);
            if (stats == null) {
                stats = Entity.newBuilder(ctrsKey).set("user_stats_logins", 0L).set("user_stats_failed", 0L).set("user_first_login", Timestamp.now()).set("user_last_login", Timestamp.now()).build();
            }
            String hashedPWD = (String) user.getString("user_pwd");
            if (hashedPWD != null && hashedPWD.equals(DigestUtils.sha512Hex(data.password)) && user.getString("user_state").equals("Active")) {
                // Password is correct
                // Get the user statistics and updates it
                Entity ustats = Entity.newBuilder(ctrsKey).set("user_stats_logins", 1L + stats.getLong("user_stats_logins")).set("user_stats_failed", 0L).set("user_first_login", stats.getTimestamp("user_first_login")).set("user_last_login", Timestamp.now()).build();

                // Return token
                AuthToken token = new AuthToken(data.username, user.getString("user_role"));
                Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(token.tokenID);
                Entity authToken = Entity.newBuilder(tokenKey).set("token_name", token.username).set("token_role", token.role).set("token_creationDate", token.creationDate).set("token_expirationDate", token.expirationDate).build();
                LOG.info("User '" + data.username + "' logged in sucessfully.");
                // Batch operation
                txn.put(authToken, ustats);
                txn.commit();
                return Response.ok(g.toJson(token)).build();
            } else {
                // Incorrect password
                Entity ustats = Entity.newBuilder(ctrsKey).set("user_stats_logins", stats.getLong("user_stats_logins")).set("user_stats_failed", 1L + stats.getLong("user_stats_failed")).set("user_first_login", stats.getTimestamp("user_first_login")).set("user_last_login", stats.getTimestamp("user_last_login")).set("user_last_attempt", Timestamp.now()).build();

                txn.put(ustats);
                txn.commit();

                LOG.warning("Wrong password for username: " + data.username);
                return Response.status(Status.FORBIDDEN).build();
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
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response checkUsernameAvailable(LoginData data) {
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);
        if (user != null && user.getString("user_pwd").equals(DigestUtils.sha512Hex(data.password))) {

            // Get the date of yesterday
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            Timestamp yesterday = Timestamp.of(cal.getTime());

            Query<Entity> query = Query.newEntityQueryBuilder().setKind("UserLog").setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(datastore.newKeyFactory().setKind("User").newKey(data.username)), PropertyFilter.ge("user_login_time", yesterday))).setOrderBy(OrderBy.desc("user_login_time")).setLimit(3).build();
            QueryResults<Entity> logs = datastore.run(query);
            List<Date> loginDates = new ArrayList<Date>();
            logs.forEachRemaining(userlog -> {
                loginDates.add(userlog.getTimestamp("user_login_time").toDate());
            });
            return Response.ok(g.toJson(loginDates)).build();

        } else {
            LOG.warning("Wrong password for username: " + data.username);
            return Response.status(Status.FORBIDDEN).build();
        }

    }

}

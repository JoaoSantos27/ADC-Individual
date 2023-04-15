package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import pt.unl.fct.di.apdc.firstwebapp.util.Role;
import pt.unl.fct.di.apdc.firstwebapp.util.State;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.ModifyData;
import pt.unl.fct.di.apdc.firstwebapp.util.PasswordData;

@Path("/modify")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ModifyResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

    private static final Gson g = new Gson();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\." + "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z" + "A-Z]{2,7}$");

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public ModifyResource() {
    } // Nothing to do here

    @POST
    @Path("/v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response doModifyV1(ModifyData data) {
        LOG.fine("Attempting to modify user: " + data.username);
        LOG.warning(data.toString());
        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.token.tokenID);
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
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
                    LOG.warning("Failed attempt to modify user: " + data.username);
                    return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
                }
                if (checkPermissions(data, user.getString("user_role"))) {
                    if (data.attribute.equals("user_privacy")) {
                        if (!data.newValue.equals("Private") && !data.newValue.equals("Public")) {
                            txn.rollback();
                            LOG.warning("Invalid privacy setting");
                            return Response.status(Status.FORBIDDEN).build();
                        }
                    }
                    if (data.attribute.equals("user_email")) {
                        Matcher matcher = EMAIL_PATTERN.matcher(data.newValue);
                        if (!matcher.matches()) {
                            txn.rollback();
                            LOG.warning("Invalid email");
                            return Response.status(Status.FORBIDDEN).build();
                        }
                    }
                    if (data.attribute.equals("user_role")) {
                        if (!data.newValue.equals(Role.USER.getValue()) && !data.newValue.equals(Role.GBO.getValue()) && !data.newValue.equals(Role.GA.getValue()) && !data.newValue.equals(Role.GS.getValue()) && !data.newValue.equals(Role.SU.getValue()) || !checkRoleModPerms(data, user.getString("user_role"))) {
                            txn.rollback();
                            LOG.warning("Invalid role");
                            return Response.status(Status.FORBIDDEN).build();
                        } else if (data.token.role.equals(user.getString("user_role"))) {
                            data.token.role = data.newValue;
                            token = Entity.newBuilder(token).set("token_role", data.token.role).build();
                            txn.update(token);
                        }


                    }
                    if (data.attribute.equals("user_state")) {
                        if (!data.newValue.equals(State.INACTIVE.getValue()) && !data.newValue.equals(State.ACTIVE.getValue()) || !checkStateModPerms(data, user.getString("user_role"))) {
                            txn.rollback();
                            LOG.warning("Invalid state");
                            return Response.status(Status.FORBIDDEN).build();
                        }
                    }
                    user = Entity.newBuilder(user).set(data.attribute, data.newValue).build();
                    txn.update(user);
                    txn.commit();
                    return Response.ok(g.toJson(data.token)).build();
                } else {
                    txn.rollback();
                    LOG.warning("You don't have permission to modify that user.");
                    return Response.status(Status.FORBIDDEN).build();
                }

            } else {
                LOG.warning("Token does not belong to you");
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } catch (

                Exception e) {
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
    @Path("/pwd")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doModifyPWD(PasswordData data) {
        LOG.fine("Attempting to modify user password of: " + data.token.username);
        // Construct the key from the username
        Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.token.tokenID);
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.token.username);
        Transaction txn = datastore.newTransaction();
        try {
            Entity token = txn.get(tokenKey);
            if (token == null) {
                // Token does not exist
                LOG.warning("Token does not exist");
                return Response.status(Status.FORBIDDEN).build();
            }
            if (data.token.username.equals(token.getString("token_name"))) {
                Entity user = txn.get(userKey);
                if ((data.newPassword == null) || (data.newPassword.length() < 8)) {
                    txn.rollback();
                    LOG.warning("Invalid new password");
                    return Response.status(Status.FORBIDDEN).build();
                }
                String hashedPWD = (String) user.getString("user_pwd");
                if (!hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
                    txn.rollback();
                    LOG.warning("Wrong current password");
                    return Response.status(Status.FORBIDDEN).build();
                }
                user = Entity.newBuilder(user).set("user_pwd", DigestUtils.sha512Hex(data.newPassword)).build();
                txn.update(user);
                txn.commit();
                return Response.ok().build();
            } else {
                LOG.warning("Token does not belong to you");
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

    private boolean checkPermissions(ModifyData data, String role) {
        return (data.token.username.equals(data.username)) || (data.token.role.equals("GBO") && role.equals("User")) || (data.token.role.equals("GA") && (role.equals("User") || role.equals("GBO"))) || (data.token.role.equals("GS") && (role.equals("User") || role.equals("GBO") || role.equals("GA")) || (data.token.role.equals("SU")));
    }

    private boolean checkRoleModPerms(ModifyData data, String role) {
        return (data.token.role.equals("GS") && (role.equals("User") || role.equals("GBO") || role.equals("GA")) || (data.token.role.equals("SU")));
    }

    private boolean checkStateModPerms(ModifyData data, String role) {
        return (data.token.username.equals(data.username)) || (data.token.role.equals("GBO") && role.equals("User")) || (data.token.role.equals("GA") && (role.equals("User") || role.equals("GBO"))) || (data.token.role.equals("GS") && (role.equals("GBO") || role.equals("GA")) || (data.token.role.equals("SU")));
    }
}

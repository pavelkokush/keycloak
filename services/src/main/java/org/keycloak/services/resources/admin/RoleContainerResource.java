package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.AdminEventBuilder;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleContainerResource extends RoleResource {
    private final RealmModel realm;
    private final RealmAuth auth;
    protected RoleContainerModel roleContainer;
    private AdminEventBuilder adminEvent;

    public RoleContainerResource(RealmModel realm, RealmAuth auth, RoleContainerModel roleContainer, AdminEventBuilder adminEvent) {
        super(realm);
        this.realm = realm;
        this.auth = auth;
        this.roleContainer = roleContainer;
        this.adminEvent = adminEvent;
    }

    /**
     * List all roles for this realm or client
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<RoleRepresentation> getRoles(@Context final UriInfo uriInfo) {
        auth.requireAny();

        Set<RoleModel> roleModels = roleContainer.getRoles();
        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roleModels) {
            roles.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        adminEvent.operation(OperationType.VIEW).resourcePath(uriInfo.getPath()).success();
        return roles;
    }

    /**
     * Create a new role for this realm or client
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRole(final @Context UriInfo uriInfo, final RoleRepresentation rep) {
        auth.requireManage();

        try {
            RoleModel role = roleContainer.addRole(rep.getName());
            role.setDescription(rep.getDescription());

            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo.getAbsolutePathBuilder()
                    .path(role.getName()).build().toString().substring(uriInfo.getBaseUri().toString().length()))
                    .representation(rep).success();

            return Response.created(uriInfo.getAbsolutePathBuilder().path(role.getName()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Role with name " + rep.getName() + " already exists");
        }
    }

    /**
     * Get a role by name
     *
     * @param roleName role's name (not id!)
     * @return
     */
    @Path("{role-name}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RoleRepresentation getRole(@Context final UriInfo uriInfo, final @PathParam("role-name") String roleName) {
        auth.requireView();

        RoleModel roleModel = roleContainer.getRole(roleName);
        if (roleModel == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }

        adminEvent.operation(OperationType.VIEW).resourcePath(uriInfo.getPath()).success();

        return getRole(roleModel);
    }

    /**
     * Delete a role by name
     *
     * @param roleName role's name (not id!)
     */
    @Path("{role-name}")
    @DELETE
    @NoCache
    public void deleteRole(@Context final UriInfo uriInfo, final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleRepresentation rep = getRole(uriInfo, roleName);
        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        deleteRole(role);

        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo.getPath()).success();

    }

    /**
     * Update a role by name
     *
     * @param roleName role's name (not id!)
     * @param rep
     * @return
     */
    @Path("{role-name}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRole(@Context final UriInfo uriInfo, final @PathParam("role-name") String roleName, final RoleRepresentation rep) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        try {
            updateRole(rep, role);

            adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo.getPath()).representation(rep).success();

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Role with name " + rep.getName() + " already exists");
        }
    }

    /**
     * Add a composite to this role
     *
     * @param roleName role's name (not id!)
     * @param roles
     */
    @Path("{role-name}/composites")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addComposites(@Context final UriInfo uriInfo, final @PathParam("role-name") String roleName, List<RoleRepresentation> roles) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        addComposites(roles, role);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo.getPath()).representation(roles).success();

    }

    /**
     * List composites of this role
     *
     * @param roleName role's name (not id!)
     * @return
     */
    @Path("{role-name}/composites")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRoleComposites(@Context final UriInfo uriInfo, final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        adminEvent.operation(OperationType.VIEW).resourcePath(uriInfo.getPath()).success();
        return getRoleComposites(role);
    }

    /**
     * Get realm-level roles of this role's composite
     *
     * @param roleName role's name (not id!)
     * @return
     */
    @Path("{role-name}/composites/realm")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getRealmRoleComposites(@Context final UriInfo uriInfo, final @PathParam("role-name") String roleName) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        adminEvent.operation(OperationType.VIEW).resourcePath(uriInfo.getPath()).success();
        return getRealmRoleComposites(role);
    }

    /**
     * An client-level roles for a specific client for this role's composite
     *
     * @param roleName role's name (not id!)
     * @param clientId
     * @return
     */
    @Path("{role-name}/composites/client/{clientId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getClientRoleComposites(@Context final UriInfo uriInfo, 
                                                           final @PathParam("role-name") String roleName,
                                                           final @PathParam("clientId") String clientId) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        ClientModel app = realm.getClientByClientId(clientId);
        if (app == null) {
            throw new NotFoundException("Could not find client: " + clientId);

        }
        adminEvent.operation(OperationType.VIEW).resourcePath(uriInfo.getPath()).success();
        return getClientRoleComposites(app, role);
    }


    /**
     * An app-level roles for a specific app for this role's composite
     *
     * @param roleName role's name (not id!)
     * @param id
     * @return
     */
    @Path("{role-name}/composites/client-by-id/{id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<RoleRepresentation> getClientByIdRoleComposites(@Context final UriInfo uriInfo,
                                                                final @PathParam("role-name") String roleName,
                                                                final @PathParam("id") String id) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        ClientModel client = realm.getClientById(id);
        if (client == null) {
            throw new NotFoundException("Could not find client: " + id);

        }
        adminEvent.operation(OperationType.VIEW).resourcePath(uriInfo.getPath()).success();
        return getClientRoleComposites(client, role);
    }


    /**
     * Remove roles from this role's composite
     *
     * @param roleName role's name (not id!)
     * @param roles roles to remove
     */
    @Path("{role-name}/composites")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteComposites(@Context final UriInfo uriInfo, 
                                   final @PathParam("role-name") String roleName,
                                   List<RoleRepresentation> roles) {
        auth.requireManage();

        RoleModel role = roleContainer.getRole(roleName);
        if (role == null) {
            throw new NotFoundException("Could not find role: " + roleName);
        }
        deleteComposites(roles, role);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo.getPath()).success();
    }


}

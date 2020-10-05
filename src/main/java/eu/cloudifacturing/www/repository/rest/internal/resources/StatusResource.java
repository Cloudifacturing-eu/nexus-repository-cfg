package eu.cloudifacturing.www.repository.rest.internal.resources;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.cloudifacturing.www.repository.rest.internal.resources.doc.StatusResourceDoc;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.status.StatusHealthCheckStore;
import org.sonatype.nexus.rest.Resource;

import com.codahale.metrics.annotation.Timed;

import static com.google.common.base.Preconditions.checkNotNull;
import static eu.cloudifacturing.www.repository.rest.APIConstants.CFGREPO_V1_API_PREFIX;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

/**
 * @since 3.next
 */
@Named
@Singleton
@Path(StatusResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class StatusResource
        extends ComponentSupport
        implements Resource, StatusResourceDoc
{
    public static final String RESOURCE_URI = CFGREPO_V1_API_PREFIX + "/health";

    private final StatusHealthCheckStore statusHealthCheckStore;

    @Inject
    public StatusResource(final StatusHealthCheckStore statusHealthCheckStore) {
        this.statusHealthCheckStore = checkNotNull(statusHealthCheckStore);
    }

    @GET
    @Timed
    @Produces("application/json")
    public Response isAvailable() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode message = mapper.createObjectNode();
        try {
            // successful write transaction represents node health, so just update via the store to verify health
            statusHealthCheckStore.markHealthCheckTime();
            return ok().build();
        }
        catch (Exception e) {
            log.error("Status health check failed, responding server is unavailable", e);
            message.put("error", e.getMessage());
            return status(Response.Status.INTERNAL_SERVER_ERROR).entity(message.toString()).build();
        }
    }
}

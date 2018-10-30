package eu.cloudifacturing.www.repository.rest.internal.resources;

import eu.cloudifacturing.www.repository.rest.api.RepositoryXO;
import eu.cloudifacturing.www.repository.rest.internal.resources.doc.RepositoriesResourceDoc;
import org.sonatype.nexus.rest.Resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static eu.cloudifacturing.www.repository.rest.APIConstants.CFGREPO_V1_API_PREFIX;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Named
@Singleton
@Path(RepositoriesResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class RepositoriesResource implements Resource, RepositoriesResourceDoc {
    public static final String RESOURCE_URI = CFGREPO_V1_API_PREFIX + "/repositories";

    private final CfgRepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

    @Inject
    public RepositoriesResource(final CfgRepositoryManagerRESTAdapter repositoryManagerRESTAdapter) {
        this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    }

    @GET
    public List<RepositoryXO> getRepositories() {
        return repositoryManagerRESTAdapter.getRepositories()
                .stream()
                .map(eu.cloudifacturing.www.repository.rest.api.RepositoryXO::fromRepository)
                .collect(toList());
    }

    @GET
    @Path("/{id}")
    public RepositoryXO getRepositoryById(@PathParam("id") final String id) {
        return RepositoryXO.fromRepository(repositoryManagerRESTAdapter.getRepository(id));
    }
}

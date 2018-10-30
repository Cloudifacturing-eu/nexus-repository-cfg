package eu.cloudifacturing.www.repository.rest.internal.resources.doc;

import eu.cloudifacturing.www.repository.rest.api.ArtefactXO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.sonatype.nexus.rest.Page;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Api(value = "CFG Search")
public interface SearchResourceDoc {
    @ApiOperation("Search artefacts")
    Page<ArtefactXO> searchArtefacts(
            @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
            final String continuationToken,
            @Context final UriInfo uriInfo);
}

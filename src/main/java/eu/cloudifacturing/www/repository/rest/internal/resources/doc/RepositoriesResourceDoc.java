package eu.cloudifacturing.www.repository.rest.internal.resources.doc;

import eu.cloudifacturing.www.repository.rest.api.RepositoryXO;
import io.swagger.annotations.*;

import java.util.List;

@Api(value = "CFG Repositories")
public interface RepositoriesResourceDoc
{
    @ApiOperation("List repositories")
    List<RepositoryXO> getRepositories();

    @ApiOperation("Get repository by id")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Insufficient permissions to get repository"),
            @ApiResponse(code = 404, message = "Repository not found")
    })
    RepositoryXO getRepositoryById(@ApiParam(value = "Id of the repository to get") final String id);
}

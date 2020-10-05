package eu.cloudifacturing.www.repository.rest.internal.resources.doc;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST API for status operations
 *
 * @since 3.next
 */
@Api("status")
public interface StatusResourceDoc
{
    /**
     * @return 200 if the server is available, 503 otherwise
     */
    @GET
    @ApiOperation("Health check endpoint for server")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Available to service requests"),
            @ApiResponse(code = 500, message = "Unavailable to service requests")
    })
    Response isAvailable();
}

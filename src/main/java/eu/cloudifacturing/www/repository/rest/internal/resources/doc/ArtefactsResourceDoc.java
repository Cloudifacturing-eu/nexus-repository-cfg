package eu.cloudifacturing.www.repository.rest.internal.resources.doc;

import eu.cloudifacturing.www.repository.rest.api.ArtefactXO;
import eu.cloudifacturing.www.repository.rest.api.MetadataXO;
import io.swagger.annotations.*;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.sonatype.nexus.rest.Page;

import java.io.IOException;
import java.util.Map;

@Api(value = "CFG Artefacts", description = "Operations to register, upload, get and delete artefacts")
public interface ArtefactsResourceDoc {
    @ApiOperation(value = "Upload a single artefact")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Insufficient permissions to upload an artefact"),
            @ApiResponse(code = 422, message = "Parameter 'repository' is required")
    })
    ArtefactXO uploadArtefact(
            @ApiParam(value = "Name of the repository to which you would like to upload the artefact", required = true)
            final String repository,
            @ApiParam(hidden = false) @MultipartForm MultipartInput multipartInput)
            throws IOException;


    @ApiOperation("List artefacts")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Insufficient permissions to list artefacts"),
            @ApiResponse(code = 422, message = "Parameter 'repository' is required")
    })
    Page<ArtefactXO> getArtefacts(
            @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
            final String continuationToken,

            @ApiParam(value = "Repository from which you would like to retrieve artefacts.", required = true)
            final String repository);

    @ApiOperation("Get a single artefact")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Insufficient permissions to get artefact"),
            @ApiResponse(code = 404, message = "Artefact not found"),
            @ApiResponse(code = 422, message = "Malformed ID")
    })
    ArtefactXO getAssetById(@ApiParam(value = "Id of the artefact to get") final String id);

    @ApiOperation(value = "Delete a single artefact")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Artefact was successfully deleted"),
            @ApiResponse(code = 403, message = "Insufficient permissions to delete artefact"),
            @ApiResponse(code = 404, message = "Artefact not found"),
            @ApiResponse(code = 422, message = "Malformed ID")
    })
    void deleteArtefact(@ApiParam(value = "Id of the artefact to delete") final String id);

    @ApiOperation("Get metadata")
    MetadataXO getArtefactMetadata(@ApiParam(value = "Id of the artefact to get") final String id);

    @ApiOperation("Add/Update artifact metadatas by ID")
    MetadataXO updateArtefactMetadata(@ApiParam(value = "ID of the artifact") final String id, final Map<String, String> metadata);

    @ApiOperation("Delete artifact metadatas by ID & Key")
    MetadataXO deleteArtefactMetadata(@ApiParam(value = "ID of the artifact") final String id, @ApiParam(value = "metadata key") final String key);
}

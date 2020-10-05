package eu.cloudifacturing.www.repository.rest.internal.resources;


import com.fasterxml.jackson.core.JsonParseException;
import eu.cloudifacturing.www.repository.rest.api.ArtefactXO;
import eu.cloudifacturing.www.repository.rest.api.MetadataXO;
import eu.cloudifacturing.www.repository.rest.api.RepositoryItemIDXO;
import eu.cloudifacturing.www.repository.rest.internal.resources.doc.ArtefactsResourceDoc;
import eu.cloudifacturing.www.repository.rest.security.CfgPermissionChecker;
import eu.cloudifacturing.www.security.cfgum.oauth.CfgumPrincipal;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.rest.ComponentUploadExtension;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.AssetStore;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecurityHelper;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static eu.cloudifacturing.www.repository.rest.APIConstants.CFGREPO_V1_API_PREFIX;
import static eu.cloudifacturing.www.repository.rest.api.ArtefactXO.fromAsset;
import static eu.cloudifacturing.www.repository.rest.api.RepositoryItemIDXO.fromString;
import static eu.cloudifacturing.www.repository.rest.internal.resources.ComponentUploadUtils.createComponentUpload;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_ACCEPTABLE;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Named
@Singleton
@Path(ArtefactsResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class ArtefactsResource extends ComponentSupport implements Resource, ArtefactsResourceDoc {
    public static final String RESOURCE_URI = CFGREPO_V1_API_PREFIX + "/artefacts";

    private final BrowseService browseService;
    private final CfgRepositoryManagerRESTAdapter repositoryManagerRESTAdapter;
    private final ContinuationTokenHelper assetContinuationTokenHelper;
    private final AssetStore assetStore;
    private final AssetEntityAdapter assetEntityAdapter;
    private final UploadManager uploadManager;
    private final UploadConfiguration uploadConfiguration;
    private final Set<ComponentUploadExtension> componentsUploadExtensions;
    private final MaintenanceService maintenanceService;
    private final CfgPermissionChecker cfgPermissionChecker;

    @Inject
    public ArtefactsResource(
            final BrowseService browseService,
            final CfgRepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
            @Named("asset") final ContinuationTokenHelper assetContinuationTokenHelper,
            final AssetStore assetStore,
            final AssetEntityAdapter assetEntityAdapter,
            final UploadManager uploadManager,
            final UploadConfiguration uploadConfiguration,
            final Set<ComponentUploadExtension> componentsUploadExtensions,
            final MaintenanceService maintenanceService,
            final CfgPermissionChecker cfgPermissionChecker){
        this.repositoryManagerRESTAdapter = repositoryManagerRESTAdapter;
        this.browseService = browseService;
        this.assetContinuationTokenHelper = assetContinuationTokenHelper;
        this.assetStore = assetStore;
        this.assetEntityAdapter = assetEntityAdapter;
        this.uploadManager = uploadManager;
        this.uploadConfiguration = uploadConfiguration;
        this.componentsUploadExtensions = checkNotNull(componentsUploadExtensions);
        this.maintenanceService = maintenanceService;
        this.cfgPermissionChecker = cfgPermissionChecker;
    }

    @GET
    public Page<ArtefactXO> getArtefacts(@QueryParam("continuationToken") final String continuationToken,
                                         @QueryParam("repository") final String repositoryId) {
        Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);
        BrowseResult<Asset> assetBrowseResult = browseService.browseAssets(
                repository,
                new QueryOptions(null, "id", "asc", 0, 10, lastIdFromContinuationToken(continuationToken)));
        List<ArtefactXO> artefactXOs = assetBrowseResult.getResults().stream()
                .map(asset -> fromAsset(asset, repository))
                .collect(toList());
        return new Page<>(artefactXOs, assetBrowseResult.getTotal() > assetBrowseResult.getResults().size() ?
                assetContinuationTokenHelper.getTokenFromId(getLast(assetBrowseResult.getResults())) : null);
    }

    @Nullable
    private String lastIdFromContinuationToken(final String continuationToken) {
        try {
            return assetContinuationTokenHelper.getIdFromToken(continuationToken);
        }
        catch (ContinuationTokenHelper.ContinuationTokenException e) {
            log.debug(e.getMessage(), e);
            throw new WebApplicationException(NOT_ACCEPTABLE);
        }
    }

    @GET
    @Path("/{id}")
    public ArtefactXO getAssetById(@PathParam("id") final String id)
    {
        RepositoryItemIDXO repositoryItemIDXO = fromString(id);
        Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());

        Asset asset = getAsset(id, repository, new DetachedEntityId(repositoryItemIDXO.getId()));
        return ArtefactXO.fromAsset(asset, repository);
    }

    private Asset getAsset(final String id, final Repository repository, final DetachedEntityId entityId)
    {
        try {
            return ofNullable(browseService
                    .getAssetById(assetEntityAdapter.recordIdentity(entityId), repository))
                    .orElseThrow(() -> new NotFoundException("Unable to locate asset with id " + id));
        }
        catch (IllegalArgumentException e) {
            log.debug("IllegalArgumentException caught retrieving asset with id {}", entityId, e);
            throw new WebApplicationException(format("Unable to process asset with id %s", entityId), UNPROCESSABLE_ENTITY);
        }
    }

    @DELETE
    @Path("/{id}")
    public void deleteArtefact(@PathParam("id")
                            final String id)
    {
        RepositoryItemIDXO repositoryItemIDXO = RepositoryItemIDXO.fromString(id);
        Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());
        DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
        Asset asset = getAsset(id, repository, entityId);
        cfgPermissionChecker.ensurePermitted(asset.attributes().child("cfg").get("groupId").toString());
        maintenanceService.deleteAsset(repository, asset);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public  ArtefactXO uploadArtefact(@QueryParam("repository") final String repositoryId, final MultipartInput multipartInput)
            throws IOException {
        if (!uploadConfiguration.isEnabled()) {
            throw new WebApplicationException(NOT_FOUND);
        }
        Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);
        String format = repository.getFormat().getValue();
        ComponentUpload componentUpload = createComponentUpload(format, multipartInput);
        cfgPermissionChecker.ensurePermitted(componentUpload.getFields().get("groupId"));

        for (ComponentUploadExtension componentUploadExtension : componentsUploadExtensions) {
            componentUploadExtension.validate(componentUpload);
        }

        try {
            UploadResponse uploadResponse = uploadManager.handle(repository, componentUpload);

            for (ComponentUploadExtension componentUploadExtension : componentsUploadExtensions) {
                componentUploadExtension.apply(repository, componentUpload, uploadResponse.getComponentIds());
            }

            if(uploadResponse.getComponentId()!=null) {
                return ArtefactXO.fromAsset(browseService.browseComponentAssets(repository,uploadResponse.getComponentId().getValue()).getResults().get(0),repository);
            } else {
                return null;
            }
        } catch (IllegalOperationException e) {
            throw new WebApplicationMessageException(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (IllegalArgumentException e){
            log.warn(e.getCause()+":" + e.getMessage());
        } catch (JsonParseException e){
            throw new WebApplicationMessageException(Response.Status.BAD_REQUEST, "Invalid JSON string for 'cfg.metadata'."+e.getMessage());
        }
        return null;
    }

    @GET
    @Path("/{id}/metadata")
    public MetadataXO getArtefactMetadata(@PathParam("id")
                                          final String id) {
        RepositoryItemIDXO repositoryItemIDXO = RepositoryItemIDXO.fromString(id);
        Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());

        DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
        Asset asset = getAsset(id, repository, entityId);
        return MetadataXO.fromAssetMetadata(asset);
    }

    @PUT
    @Path("/{id}/metadata")
    @Consumes(MediaType.APPLICATION_JSON)
    public MetadataXO updateArtefactMetadata(@PathParam("id")
                                             final String id, final Map<String, String> metadata) {

        RepositoryItemIDXO repositoryItemIDXO = RepositoryItemIDXO.fromString(id);
        Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());

        DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
        Asset asset = getAsset(id, repository, entityId);
        cfgPermissionChecker.ensurePermitted(asset.attributes().child("cfg").get("groupId").toString());
        metadata.forEach((k,v)->{
            asset.attributes().child("metadata").set(k,v);
        });
        assetStore.save(asset);
        return MetadataXO.fromAssetMetadata(asset);
    }

    @DELETE
    @Path("/{id}/metadata/{key}")
    public MetadataXO deleteArtefactMetadata(@PathParam("id") final String id, @PathParam("key") final String key) {
        RepositoryItemIDXO repositoryItemIDXO = RepositoryItemIDXO.fromString(id);
        Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());
        DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
        Asset asset = getAsset(id, repository, entityId);
        cfgPermissionChecker.ensurePermitted(asset.attributes().child("cfg").get("groupId").toString());
        asset.attributes().child("metadata").remove(key);
        assetStore.save(asset);
        return MetadataXO.fromAssetMetadata(asset);
    }
}

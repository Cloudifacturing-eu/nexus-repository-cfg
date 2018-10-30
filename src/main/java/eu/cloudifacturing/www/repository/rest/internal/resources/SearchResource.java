package eu.cloudifacturing.www.repository.rest.internal.resources;

import com.google.common.annotations.VisibleForTesting;
import eu.cloudifacturing.www.repository.rest.CfgAssetMapUtils;
import eu.cloudifacturing.www.repository.rest.CfgTokenEncoder;
import eu.cloudifacturing.www.repository.rest.api.ArtefactXO;
import eu.cloudifacturing.www.repository.rest.internal.resources.doc.SearchResourceDoc;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.SearchUtils;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static eu.cloudifacturing.www.repository.rest.APIConstants.CFGREPO_V1_API_PREFIX;
import static eu.cloudifacturing.www.repository.rest.api.ArtefactXO.fromElasticSearchMap;
import static eu.cloudifacturing.www.repository.rest.internal.resources.SearchResource.RESOURCE_URI;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;

@Named
@Singleton
@Path(RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SearchResource extends ComponentSupport
        implements Resource, SearchResourceDoc {
    public static final String RESOURCE_URI = CFGREPO_V1_API_PREFIX + "/search";
    private final SearchUtils searchUtils;
    private final CfgTokenEncoder tokenEncoder;
    private final SearchService searchService;
    private final CfgAssetMapUtils assetMapUtils;
    private static final int PAGE_SIZE = 50;

    @Inject
    public SearchResource(final SearchUtils searchUtils,
                          final CfgTokenEncoder tokenEncoder,
                          final SearchService searchService,
                          final CfgAssetMapUtils assetMapUtils){
        this.searchUtils = searchUtils;
        this.tokenEncoder = tokenEncoder;
        this.searchService = searchService;
        this.assetMapUtils = assetMapUtils;
    }

    @GET
    @Path("/artefacts")
    public Page<ArtefactXO> searchArtefacts(@QueryParam("continuationToken") final String continuationToken, @Context final UriInfo uriInfo) {
        QueryBuilder query = searchUtils.buildQuery(uriInfo);
        int from = tokenEncoder.decode(continuationToken, query);
        List<ArtefactXO> artefactXOs = retrieveArtefacts(query, uriInfo, from);
        return new Page<>(artefactXOs, artefactXOs.size() == PAGE_SIZE?tokenEncoder.encode(from,PAGE_SIZE,query):null);
    }

    private List<ArtefactXO> retrieveArtefacts(final QueryBuilder query, final UriInfo uriInfo, final int from){
        SearchResponse response = searchService.search(query,emptyList(), from, PAGE_SIZE);
        MultivaluedMap<String, String> assetParams = getAssetParams(uriInfo);
        return Arrays.stream(response.getHits().hits())
                .flatMap(hit -> extractArtefacts(hit, assetParams))
                .collect(toList());
    }

    private Stream<ArtefactXO> extractArtefacts(final SearchHit componentHit,final MultivaluedMap<String, String> assetParams){
        Map<String, Object> componentMap = checkNotNull(componentHit.getSource());
        Repository repository = searchUtils.getRepository((String) componentMap.get(REPOSITORY_NAME));
        List<Map<String, Object>> assets = (List<Map<String, Object>>) componentMap.get("assets");
        if (assets == null) {
            return Stream.empty();
        }
        return assets.stream()
                .filter(assetMap -> assetMapUtils.filterAsset(assetMap, assetParams))
                .map(asset -> fromElasticSearchMap(asset, repository));
    }

    @VisibleForTesting
    MultivaluedMap<String, String> getAssetParams(final UriInfo uriInfo) {
        return uriInfo.getQueryParameters()
                .entrySet().stream()
                .filter(t -> searchUtils.isAssetSearchParam(t.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> {
                    throw new IllegalStateException(format("Duplicate key %s", u));
                }, MultivaluedHashMap::new));
    }
}

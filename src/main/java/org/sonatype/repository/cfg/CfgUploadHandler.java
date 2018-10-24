package org.sonatype.repository.cfg;

import com.google.common.collect.Lists;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.upload.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.repository.cfg.internal.CfgFormat;
import org.sonatype.repository.cfg.internal.hosted.CfgHostedFacet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.join;
import static java.util.Collections.emptyMap;

@Named(CfgFormat.NAME)
@Singleton
public class CfgUploadHandler extends UploadHandlerSupport
{
    private static final String FILENAME = "filename";

    //private static final String DIRECTORY = "directory";

    //private static final String DIRECTORY_HELP_TEXT = "Destination for uploaded files (e.g. /path/to/files/)";

    public static final String GROUP_ID = "groupId";

    private static final String GROUP_ID_HELP_TEXT = "The unique identification for the ISV. It can be the ISV’s domain name, e.g. www.dfg.eu.";

    public static final String ENGINE_ID = "engineId";

    private static final String ENGINE_ID_HELP_TEXT = "The unique identification for the execution engine, e.g. cloudflow, cloudbroker, flowster";

    public static final String PROJECT_ID = "projectId";

    private static final String PROJECT_ID_HELP_TEXT = "the unique identification for the project";

    public static final String VERSION = "version";

    public static final String SERVICE_ID = "serviceId";

    private static final String SERVICE_ID_HELP_TEXT = "the unique identification for the reference service provided by the ISV in CFG platform, e.g. workflow, job, instance ...";

    public static final String SERVICE_URL = "serviceUrl";

    private static final String SERVICE_URL_HELP_TEXT = "the url endpoint for running the service on external execution platform";

    public static final String SERVICE_CONFIGURATION = "serviceConf";

    private static final String SERVICE_CONFIGURATION_HELP_TEXT = "the configuration";

    public static final String SERVICE_CONFIGURATION_TYPE = "serviceConfType";

    private static final String SERVICE_CONFIGURATION_TYPE_HELP_TEXT = "the configuration type, e.g. string, XML, YAML ...";

    public static final String METADATA = "metadata";

    private static final String METADATA_HELP_TEXT = "json string of metadata, key,value";

    private static final String FIELD_GROUP_NAME = "Component attributes";

    private UploadDefinition definition;

    private final ContentPermissionChecker contentPermissionChecker;

    private final VariableResolverAdapter variableResolverAdapter;

    @Inject
    public CfgUploadHandler(final ContentPermissionChecker contentPermissionChecker,
                            @Named("simple") final VariableResolverAdapter variableResolverAdapter,
                            final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
    {
        super(uploadDefinitionExtensions);
        this.contentPermissionChecker = contentPermissionChecker;
        this.variableResolverAdapter = variableResolverAdapter;
    }

    @Override
    public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
        CfgHostedFacet facet = repository.facet(CfgHostedFacet.class);

        String basePath = createBasePath(upload.getFields().get(GROUP_ID).trim(), upload.getFields().get(ENGINE_ID).trim(), upload.getFields().get(PROJECT_ID).trim(), upload.getFields().get(VERSION).trim());

        AttributesMap attributes = new AttributesMap();
        attributes.set(GROUP_ID,upload.getFields().get(GROUP_ID).trim());
        attributes.set(ENGINE_ID,upload.getFields().get(ENGINE_ID).trim());
        attributes.set(PROJECT_ID,upload.getFields().get(PROJECT_ID).trim());
        attributes.set(VERSION,upload.getFields().get(VERSION).trim());
        attributes.set(SERVICE_ID,upload.getFields().get(SERVICE_ID).trim());
        attributes.set(SERVICE_URL,upload.getFields().get(SERVICE_URL).trim());
        attributes.set(SERVICE_CONFIGURATION,upload.getFields().get(SERVICE_CONFIGURATION).trim());
        attributes.set(SERVICE_CONFIGURATION_TYPE,upload.getFields().get(SERVICE_CONFIGURATION_TYPE).trim());
        attributes.set(METADATA,upload.getFields().get(METADATA).trim());


        return TransactionalStoreBlob.operation.withDb(repository.facet(StorageFacet.class).txSupplier())
                .throwing(IOException.class).call(() -> {

                    //Data holders for populating the UploadResponse
                    List<Content> responseContents = Lists.newArrayList();
                    List<String> assetPaths = Lists.newArrayList();

                    for (AssetUpload asset : upload.getAssetUploads()) {
                        String path = normalizePath(basePath + '/' + asset.getFields().get(FILENAME).trim());

                        ensurePermitted(repository.getName(), CfgFormat.NAME, path, emptyMap());

                        Content content = facet.put(path, attributes, asset.getPayload());

                        responseContents.add(content);
                        assetPaths.add(path);
                    }

                    return new UploadResponse(responseContents, assetPaths);
                });
    }

    private String normalizePath(final String path) {
        String result = path.replaceAll("/+", "/");

        if (result.startsWith("/")) {
            result = result.substring(1);
        }

        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private String createBasePath(final String groupId, final String engineId, final String artifactId, final String version) {
        List<String> parts = newArrayList(groupId.split("\\."));
        parts.addAll(Arrays.asList(engineId, artifactId, version));
        return join("/", parts);
    }

    @Override
    public UploadDefinition getDefinition() {
        if (definition == null) {
            List<UploadFieldDefinition> componentFields = Arrays.asList(
                    new UploadFieldDefinition(GROUP_ID, GROUP_ID_HELP_TEXT, false, UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(ENGINE_ID, ENGINE_ID_HELP_TEXT, false, UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(PROJECT_ID, PROJECT_ID_HELP_TEXT,false, UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(VERSION, false, UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(SERVICE_ID, SERVICE_ID_HELP_TEXT,false, UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(SERVICE_URL,SERVICE_URL_HELP_TEXT,false,UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(SERVICE_CONFIGURATION,SERVICE_CONFIGURATION_HELP_TEXT,false,UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(SERVICE_CONFIGURATION_TYPE,SERVICE_CONFIGURATION_TYPE_HELP_TEXT,false,UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME),
                    new UploadFieldDefinition(METADATA, true, UploadFieldDefinition.Type.STRING, FIELD_GROUP_NAME));
            List<UploadFieldDefinition> assetFields = Arrays.asList(
                    new UploadFieldDefinition(FILENAME, false, UploadFieldDefinition.Type.STRING));

            definition = getDefinition(CfgFormat.NAME, true,
                    componentFields,
                    assetFields,
                    new UploadRegexMap("(.*)", FILENAME));
        }
        return definition;
    }

    @Override
    public VariableResolverAdapter getVariableResolverAdapter() {
        return variableResolverAdapter;
    }

    @Override
    public ContentPermissionChecker contentPermissionChecker() {
        return contentPermissionChecker;
    }
}

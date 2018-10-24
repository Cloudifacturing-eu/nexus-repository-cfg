package org.sonatype.repository.cfg.internal.hosted;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.cfg.internal.CfgFormat;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.repository.cfg.CfgUploadHandler.*;

@Named
public class CfgHostedFacetImpl extends FacetSupport
        implements CfgHostedFacet
{
    private static final List<HashAlgorithm> hashAlgorithms = Arrays.asList(MD5, SHA1);

    // TODO: raw does not have config, this method is here only to have this bundle do Import-Package org.sonatype.nexus.repository.config
    // TODO: as FacetSupport subclass depends on it. Actually, this facet does not need any kind of configuration
    // TODO: it's here only to circumvent this OSGi/maven-bundle-plugin issue.
    @Override
    protected void doValidate(final Configuration configuration) throws Exception {
        // empty
    }

    @Nullable
    @Override
    @TransactionalTouchBlob
    public Content get(final String path) {
        StorageTx tx = UnitOfWork.currentTx();

        final Asset asset = findAsset(tx, path);
        if (asset == null) {
            return null;
        }
        if (asset.markAsDownloaded()) {
            tx.saveAsset(asset);
        }

        final Blob blob = tx.requireBlob(asset.requireBlobRef());
        return toContent(asset, blob);
    }

    @Override
    public Content put(final String path, final AttributesMap attributesMap, final Payload content) throws IOException {
        StorageFacet storageFacet = facet(StorageFacet.class);
        try (TempBlob tempBlob = storageFacet.createTempBlob(content, hashAlgorithms)) {
            return doPutContent(path, tempBlob, attributesMap, content);
        }
    }

    @TransactionalStoreBlob
    protected Content doPutContent(final String path, final TempBlob tempBlob, final AttributesMap attributesMap, final Payload payload)
            throws IOException
    {
        StorageTx tx = UnitOfWork.currentTx();

        Asset asset = getOrCreateAsset(getRepository(), path, attributesMap.get(GROUP_ID).toString(), path);

        AttributesMap contentAttributes = null;
        if (payload instanceof Content) {
            contentAttributes = ((Content) payload).getAttributes();
        }
        Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));

        asset.attributes().child(CfgFormat.NAME).set(GROUP_ID,attributesMap.get(GROUP_ID));
        asset.attributes().child(CfgFormat.NAME).set(ENGINE_ID,attributesMap.get(ENGINE_ID));
        asset.attributes().child(CfgFormat.NAME).set(PROJECT_ID,attributesMap.get(PROJECT_ID));
        asset.attributes().child(CfgFormat.NAME).set(VERSION,attributesMap.get(VERSION));
        asset.attributes().child(CfgFormat.NAME).set(SERVICE_ID,attributesMap.get(SERVICE_ID));
        asset.attributes().child(CfgFormat.NAME).set(SERVICE_URL,attributesMap.get(SERVICE_URL));
        asset.attributes().child(CfgFormat.NAME).set(SERVICE_CONFIGURATION,attributesMap.get(SERVICE_CONFIGURATION));
        asset.attributes().child(CfgFormat.NAME).set(SERVICE_CONFIGURATION_TYPE,attributesMap.get(SERVICE_CONFIGURATION_TYPE));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadata = mapper.readTree((String) attributesMap.get(METADATA));
        if(metadata!=null) {
            try {
                Iterator<String> metadataKeys = metadata.fieldNames();
                while (metadataKeys.hasNext()) {
                    String key = metadataKeys.next();
                    System.out.println(metadata.get(key).asText());
                    asset.attributes().child(METADATA).set(key, metadata.get(key).asText());
                }
            } catch (Exception e){
                log.warn(e.getMessage());
            }
        }

        AssetBlob assetBlob = tx.setBlob(
                asset,
                path,
                tempBlob,
                null,
                payload.getContentType(),
                false
        );

        tx.saveAsset(asset);

        return toContent(asset, assetBlob.getBlob());
    }

    @TransactionalStoreMetadata
    public Asset getOrCreateAsset(final Repository repository, final String componentName, final String componentGroup,
                                  final String assetName) {
        final StorageTx tx = UnitOfWork.currentTx();

        final Bucket bucket = tx.findBucket(getRepository());
        Component component = tx.findComponentWithProperty(P_NAME, componentName, bucket);
        Asset asset;
        if (component == null) {
            // CREATE
            component = tx.createComponent(bucket, getRepository().getFormat())
                    .group(componentGroup)
                    .name(componentName);

            tx.saveComponent(component);

            asset = tx.createAsset(bucket, component);
            asset.name(assetName);
        }
        else {
            // UPDATE
            asset = tx.firstAsset(component);
        }

        asset.markAsDownloaded();

        return asset;
    }

    @Override
    @TransactionalDeleteBlob
    public boolean delete(final String path) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        final Component component = findComponent(tx, tx.findBucket(getRepository()), path);
        if (component == null) {
            return false;
        }

        tx.deleteComponent(component);
        return true;
    }

    @Override
    @TransactionalTouchMetadata
    public void setCacheInfo(final String path, final Content content, final CacheInfo cacheInfo) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(getRepository());

        // by EntityId
        Asset asset = Content.findAsset(tx, bucket, content);
        if (asset == null) {
            // by format coordinates
            Component component = tx.findComponentWithProperty(P_NAME, path, bucket);
            if (component != null) {
                asset = tx.firstAsset(component);
            }
        }
        if (asset == null) {
            log.debug("Attempting to set cache info for non-existent raw component {}", path);
            return;
        }

        log.debug("Updating cacheInfo of {} to {}", path, cacheInfo);
        CacheInfo.applyToAsset(asset, cacheInfo);
        tx.saveAsset(asset);
    }

    private Component findComponent(StorageTx tx, Bucket bucket, String path) {
        return tx.findComponentWithProperty(P_NAME, path, bucket);
    }

    private Asset findAsset(StorageTx tx, String path) {
        return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
    }

    private Content toContent(final Asset asset, final Blob blob) {
        final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
        Content.extractFromAsset(asset, hashAlgorithms, content.getAttributes());
        return content;
    }
}
